package org.hbt.doe.codeforcereact.records;

import java.util.List;

public record CanvasChange(String source, List<Pixel> changedPixels) { }
