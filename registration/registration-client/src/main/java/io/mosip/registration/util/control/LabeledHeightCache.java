package io.mosip.registration.util.control;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.DoubleUnaryOperator;

import javafx.geometry.Insets;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.text.Font;

/**
 * Caches the width-dependent min/pref height of wrapping labels.
 * <p>
 * JavaFX only caches Parent sizes for width -1, so every layout pass of the demographic
 * FlowPane re-shapes every label text (DirectWrite for Sinhala/Tamil) several times per
 * nesting level. Values are reused only while text, font and layout-affecting properties
 * are unchanged; labels with a graphic are never cached.
 */
public final class LabeledHeightCache {

	private static final int MAX_WIDTHS = 16;

	private final Labeled labeled;
	private final Map<Double, Double> minHeights = new HashMap<>();
	private final Map<Double, Double> prefHeights = new HashMap<>();

	private String text;
	private Font font;
	private boolean wrapText;
	private double lineSpacing;
	private double graphicTextGap;
	private ContentDisplay contentDisplay;
	private Insets insets;
	private Insets labelPadding;

	private LabeledHeightCache(Labeled labeled) {
		this.labeled = labeled;
	}

	private double min(double width, DoubleUnaryOperator compute) {
		return cached(minHeights, width, compute);
	}

	private double pref(double width, DoubleUnaryOperator compute) {
		return cached(prefHeights, width, compute);
	}

	private double cached(Map<Double, Double> heights, double width, DoubleUnaryOperator compute) {
		if (labeled.getSkin() == null || labeled.getGraphic() != null) {
			return compute.applyAsDouble(width);
		}
		if (!sameSignature()) {
			minHeights.clear();
			prefHeights.clear();
			captureSignature();
		}
		Double height = heights.get(width);
		if (height == null) {
			if (heights.size() >= MAX_WIDTHS) {
				heights.clear();
			}
			height = compute.applyAsDouble(width);
			heights.put(width, height);
		}
		return height;
	}

	private boolean sameSignature() {
		return Objects.equals(text, labeled.getText()) && Objects.equals(font, labeled.getFont())
				&& wrapText == labeled.isWrapText() && lineSpacing == labeled.getLineSpacing()
				&& graphicTextGap == labeled.getGraphicTextGap() && contentDisplay == labeled.getContentDisplay()
				&& Objects.equals(insets, labeled.getInsets()) && Objects.equals(labelPadding, labeled.getLabelPadding());
	}

	private void captureSignature() {
		text = labeled.getText();
		font = labeled.getFont();
		wrapText = labeled.isWrapText();
		lineSpacing = labeled.getLineSpacing();
		graphicTextGap = labeled.getGraphicTextGap();
		contentDisplay = labeled.getContentDisplay();
		insets = labeled.getInsets();
		labelPadding = labeled.getLabelPadding();
	}

	public static class CachedLabel extends Label {
		private static final String TYPE_SELECTOR = "Label";

		private final LabeledHeightCache cache = new LabeledHeightCache(this);

		@Override
		protected double computeMinHeight(double width) {
			return cache.min(width, super::computeMinHeight);
		}

		@Override
		protected double computePrefHeight(double width) {
			return cache.pref(width, super::computePrefHeight);
		}

		@Override
		public String getTypeSelector() {
			return TYPE_SELECTOR;
		}
	}

	public static class CachedHyperlink extends Hyperlink {
		private static final String TYPE_SELECTOR = "Hyperlink";

		private final LabeledHeightCache cache = new LabeledHeightCache(this);

		@Override
		protected double computeMinHeight(double width) {
			return cache.min(width, super::computeMinHeight);
		}

		@Override
		protected double computePrefHeight(double width) {
			return cache.pref(width, super::computePrefHeight);
		}

		@Override
		public String getTypeSelector() {
			return TYPE_SELECTOR;
		}
	}
}
