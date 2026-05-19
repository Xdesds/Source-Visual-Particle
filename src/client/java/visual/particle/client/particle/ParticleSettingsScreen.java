package visual.particle.client.particle;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;

public class ParticleSettingsScreen extends Screen {
	private static final int PANEL_WIDTH = 330;
	private final Screen parent;
	private final VisualParticleSystem.Settings settings;

	public ParticleSettingsScreen(Screen parent) {
		super(Text.literal("Visual Particle Settings"));
		this.parent = parent;
		this.settings = VisualParticleSystem.getInstance().getSettings();
	}

	@Override
	protected void init() {
		int x = (width - PANEL_WIDTH) / 2;
		int y = 42;
		int buttonWidth = 158;
		int gap = 8;

		addDrawableChild(toggleButton(x, y, buttonWidth, "Enabled", () -> settings.enabled, value -> settings.enabled = value));
		addDrawableChild(cycleButton(x + buttonWidth + gap, y, buttonWidth, "Mode", settings.particleMode.name(), () -> {
			settings.particleMode = next(settings.particleMode);
			rebuildWidgets();
		}));

		y += 24;
		addDrawableChild(cycleButton(x, y, buttonWidth, "Glow", settings.glowMode.name(), () -> {
			settings.glowMode = next(settings.glowMode);
			rebuildWidgets();
		}));
		addDrawableChild(toggleButton(x + buttonWidth + gap, y, buttonWidth, "Random color", () -> settings.randomColor, value -> settings.randomColor = value));

		y += 32;
		addDrawableChild(toggleButton(x, y, buttonWidth, "Attack", () -> settings.attackTrigger, value -> settings.attackTrigger = value));
		addDrawableChild(toggleButton(x + buttonWidth + gap, y, buttonWidth, "Totem", () -> settings.totemTrigger, value -> settings.totemTrigger = value));

		y += 24;
		addDrawableChild(toggleButton(x, y, buttonWidth, "Walk", () -> settings.walkTrigger, value -> settings.walkTrigger = value));
		addDrawableChild(toggleButton(x + buttonWidth + gap, y, buttonWidth, "Projectiles", () -> settings.projectileTrigger, value -> settings.projectileTrigger = value));

		y += 34;
		addSlider(x, y, "Attack amount", 10.0, 80.0, () -> settings.attackAmount, value -> settings.attackAmount = (int) Math.round(value), value -> Integer.toString((int) Math.round(value)));
		y += 24;
		addSlider(x, y, "Walk amount", 0.0, 60.0, () -> settings.walkAmount, value -> settings.walkAmount = (int) Math.round(value), value -> Integer.toString((int) Math.round(value)));
		y += 24;
		addSlider(x, y, "Spread", 0.2, 3.0, () -> settings.spread, value -> settings.spread = value.floatValue(), value -> format(value));
		y += 24;
		addSlider(x, y, "Speed", 0.1, 4.0, () -> settings.speed, value -> settings.speed = value.floatValue(), value -> format(value));
		y += 24;
		addSlider(x, y, "Life time", 0.3, 8.0, () -> settings.lifeTime, value -> settings.lifeTime = value.floatValue(), value -> format(value) + "s");
		y += 24;
		addSlider(x, y, "Size", 0.2, 2.0, () -> settings.size, value -> settings.size = value.floatValue(), value -> format(value));
		y += 24;
		addSlider(x, y, "Glow size", 0.5, 12.0, () -> settings.glowSize, value -> settings.glowSize = value.floatValue(), value -> format(value));

		y += 34;
		addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), button -> {
			settings.reset();
			rebuildWidgets();
		}).dimensions(x, y, buttonWidth, 20).build());
		addDrawableChild(ButtonWidget.builder(Text.literal("Done"), button -> close()).dimensions(x + buttonWidth + gap, y, buttonWidth, 20).build());
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 18, 0xFFFFFFFF);
	}

	@Override
	public void close() {
		if (client != null) {
			client.setScreen(parent);
		}
	}

	private ButtonWidget toggleButton(int x, int y, int width, String label, BooleanSupplier getter, Consumer<Boolean> setter) {
		return ButtonWidget.builder(toggleText(label, getter.getAsBoolean()), button -> {
			setter.accept(!getter.getAsBoolean());
			button.setMessage(toggleText(label, getter.getAsBoolean()));
		}).dimensions(x, y, width, 20).build();
	}

	private ButtonWidget cycleButton(int x, int y, int width, String label, String value, Runnable onPress) {
		return ButtonWidget.builder(Text.literal(label + ": " + value), button -> onPress.run())
				.dimensions(x, y, width, 20)
				.build();
	}

	private void addSlider(int x, int y, String label, double min, double max, DoubleSupplier getter, Consumer<Double> setter, DoubleFunction<String> formatter) {
		addDrawableChild(new SettingSliderWidget(x, y, PANEL_WIDTH, 20, label, min, max, getter, setter, formatter));
	}

	private void rebuildWidgets() {
		clearChildren();
		init();
	}

	private static Text toggleText(String label, boolean value) {
		return Text.literal(label + ": " + (value ? "ON" : "OFF"));
	}

	private static Particle3D.ParticleMode next(Particle3D.ParticleMode mode) {
		Particle3D.ParticleMode[] values = Particle3D.ParticleMode.values();
		return values[(mode.ordinal() + 1) % values.length];
	}

	private static Particle3D.GlowMode next(Particle3D.GlowMode mode) {
		Particle3D.GlowMode[] values = Particle3D.GlowMode.values();
		return values[(mode.ordinal() + 1) % values.length];
	}

	private static String format(double value) {
		return String.format(Locale.ROOT, "%.1f", value);
	}

	@FunctionalInterface
	private interface BooleanSupplier {
		boolean getAsBoolean();
	}

	private static final class SettingSliderWidget extends SliderWidget {
		private final String label;
		private final double min;
		private final double max;
		private final DoubleSupplier getter;
		private final Consumer<Double> setter;
		private final DoubleFunction<String> formatter;

		private SettingSliderWidget(int x, int y, int width, int height, String label, double min, double max, DoubleSupplier getter, Consumer<Double> setter, DoubleFunction<String> formatter) {
			super(x, y, width, height, Text.empty(), normalize(getter.getAsDouble(), min, max));
			this.label = label;
			this.min = min;
			this.max = max;
			this.getter = getter;
			this.setter = setter;
			this.formatter = formatter;
			updateMessage();
		}

		@Override
		protected void updateMessage() {
			setMessage(Text.literal(label + ": " + formatter.apply(getter.getAsDouble())));
		}

		@Override
		protected void applyValue() {
			setter.accept(min + (max - min) * value);
			updateMessage();
		}

		private static double normalize(double value, double min, double max) {
			return Math.max(0.0, Math.min(1.0, (value - min) / (max - min)));
		}
	}
}
