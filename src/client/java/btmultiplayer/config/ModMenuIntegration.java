package btmultiplayer.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

public class ModMenuIntegration implements ModMenuApi {
    private static final ModConfig config = ModConfig.getInstance();

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new ConfigScreen(parent);
    }

    public static class ConfigScreen extends Screen {
        private final Screen parent;

        public ConfigScreen(Screen parent) {
            super(Text.translatable(
                    "bluetoothmultiplayer.config.title"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            // 明确指定泛型类型，解决类型不匹配问题
            CyclingButtonWidget<ModConfig.PrecisionLevel> precisionButton = CyclingButtonWidget.<ModConfig.PrecisionLevel>builder(
                            level -> Text.translatable(
                                    "bluetoothmultiplayer.config.precision." + level.name().toLowerCase())
                    )
                    .values(ModConfig.PrecisionLevel.values())
                    .initially(config.packetPrecision)
                    .build(
                            this.width / 2 - 100,
                            this.height / 2 - 20,
                            200,
                            20,
                            Text.translatable(
                                    "bluetoothmultiplayer.config.precision.title"),
                            (button, value) -> config.packetPrecision = value
                    );

            ButtonWidget closeButton = ButtonWidget.builder(
                            Text.translatable(
                                    "bluetoothmultiplayer.config.close"),
                            button -> {
                                if (this.client != null) {
                                    this.client.setScreen(parent);
                                }
                            }
                    )
                    .dimensions(this.width / 2 - 100, this.height / 2 + 20, 200, 20)
                    .build();

            this.addDrawableChild(precisionButton);
            this.addDrawableChild(closeButton);
        }

        // 适配1.20+的renderBackground方法
        @Override
        public void renderBackground(DrawContext context) {
            super.renderBackground(context);
        }

        // 适配1.20+的render方法
        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            this.renderBackground(context);
            // 使用新的绘制方法
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    this.title,
                    this.width / 2,
                    20,
                    0xFFFFFF
            );
            super.render(context, mouseX, mouseY, delta);
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return super.shouldCloseOnEsc();
        }
    }
}
