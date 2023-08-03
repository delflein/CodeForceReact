package org.hbt.doe.codeforcereact;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.hbt.doe.codeforcereact.records.CanvasChange;
import org.hbt.doe.codeforcereact.records.CanvasDimensions;
import org.hbt.doe.codeforcereact.records.Pixel;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.List;

@SpringBootApplication
@Slf4j
public class CodeforceReactApplication {

    private final ObjectMapper objectMapper;
    private final WebSocketClient client;
    private final Random random;

    private static final URI SOCKET_URI = URI.create("ws://CLW345.corp.hbt.de:8080/streams/changes");

    public CodeforceReactApplication() throws InterruptedException, IOException {
        this.random = new Random();
        this.client = new ReactorNettyWebSocketClient();
        this.objectMapper = new ObjectMapper();

        URL imgUrl = CodeforceReactApplication.class.getResource("/thumb-HBT.png");
        BufferedImage img = ImageIO.read(imgUrl);

        //drawImg(img);
        //germany();
        //fluxInterval();
        challenge();
    }

    private void drawImg(BufferedImage img) throws InterruptedException {
        var pixels = new ArrayList<Pixel>();
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                Color color = new Color(img.getRGB(x, y), true);
                if (color.getAlpha() > 10) {
                    var pixel = new Pixel(x, y, color.getRed(), color.getGreen(), color.getBlue());
                    pixels.add(pixel);
                }
            }
        }
    }

    private Pixel createTeamPixel() {
        var x = random.nextInt(240);
        var y = random.nextInt(128);
        System.out.printf("X: %d, Y: %d%n", x, y);
        return new Pixel(x, y, 240, 24, 236);
    }

    private CanvasChange getTeamChange() {
        var pixels = new ArrayList<Pixel>();
        for (int i = 0; i < random.nextInt(100); i++) {
            pixels.add(createTeamPixel());
        }
        return new CanvasChange("Windrunner", pixels);
    }

    private Mono<CanvasDimensions> getDummyDimensions() {
        return Mono.just(new CanvasDimensions(240, 128));
    }

    private void fluxInterval() {
        client.execute(SOCKET_URI, session -> {
            var flux = Flux.interval(Duration.ofMillis(20L))
                    .zipWith(getDummyDimensions())
                    .map(tuple2 -> messageForChange(session, getTeamChange()));
            return session.send(flux);
        }).subscribe();
    }

    private void partitionedSend(List<Pixel> pixels) throws InterruptedException {
        var partitions = Lists.partition(pixels, 100);
        for (List<Pixel> partition : partitions) {
            var change = new CanvasChange("Windrunner", partition);
            client.execute(SOCKET_URI, session -> session.send(Mono.just(messageForChange(session, change)))).subscribe();
            Thread.sleep(200);
        }
    }

    private Map<String, Long> players = new HashMap<>();
    private long total;

    private CanvasChange logChanges(CanvasChange change) {
        var player = change.source();
        players.putIfAbsent(player, 0L);
        change.changedPixels().forEach(pixel -> {
            players.put(player, players.get(player) + 1);
            total++;
        });
        for (Map.Entry<String, Long> stringLongEntry : players.entrySet()) {
            System.out.printf("%s: %d%n", stringLongEntry.getKey(), stringLongEntry.getValue());
        }
        System.out.printf("Total: %d%n", total);
        return change;
    }

    private void challenge() throws InterruptedException {
        client.execute(SOCKET_URI, session -> {
            Flux<WebSocketMessage> changes = session
                    .receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .mapNotNull(s -> fromJSONString(s, CanvasChange.class))
                    .log()
                    .map(this::logChanges)
                    .filter(change -> change.source().trim().equalsIgnoreCase("certify"))
                    .map(change -> messageForChange(session,
                            new CanvasChange("Windrunner", change.changedPixels()
                                    .stream()
                                    .map(Pixel::toTeamColor)
                                    .toList())))
                    .delayElements(Duration.ofMillis(500L));
            return session.send(changes);
        }).subscribe();
    }

    private <T> T fromJSONString(String value, Class<T> clazz) {
        try {
            return objectMapper.readValue(value, clazz);
        } catch (Exception error) {
            //log.error("Failed to conver to object", error);
            return null;
        }
    }

    private WebSocketMessage messageForChange(WebSocketSession session, CanvasChange change) {
        try {
            return session.textMessage(objectMapper.writeValueAsString(change));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(CodeforceReactApplication.class, args);
    }


    public void germany() throws InterruptedException {
        var blackPixels = new ArrayList<Pixel>();
        var redPixels = new ArrayList<Pixel>();
        var goldPixels = new ArrayList<Pixel>();

        for (int i = 0; i <= 240; i++) {
            for (int j = 0; j <= 42; j++) {
                var p = new Pixel(i, j, 0, 0, 0);
                blackPixels.add(p);
            }
            for (int j = 43; j <= 84; j++) {
                var p = new Pixel(i, j, 255, 0, 0);
                redPixels.add(p);
            }
            for (int j = 85; j <= 128; j++) {
                var p = new Pixel(i, j, 255, 204, 0);
                goldPixels.add(p);
            }
        }
        partitionedSend(blackPixels);
        partitionedSend(redPixels);
        partitionedSend(goldPixels);
    }
}
