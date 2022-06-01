package potionstudios.byg.client.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FastColor;
import potionstudios.byg.BYGConstants;
import potionstudios.byg.client.KillClient;
import potionstudios.byg.client.gui.biomepedia.widget.ScrollableText;
import potionstudios.byg.config.BYGConfigHandler;
import potionstudios.byg.config.ConfigVersionTracker;
import potionstudios.byg.mixin.access.client.ScreenAccess;
import potionstudios.byg.server.command.UpdateConfigsCommand;
import potionstudios.byg.util.ModPlatform;

import java.io.PrintWriter;
import java.io.StringWriter;

public class BYGConfigLoadFailureScreen extends Screen {
    private static final int VALIDATION_COOLDOWN = 100;
    private final Exception exception;
    private final Runnable runnable;
    ScrollableText text;
    private int validationCountDown;
    private Button validateConfigs;
    private final Component validateConfigsMessage = new TextComponent("Validate Configs");
    private int errorBoxTop;

    public BYGConfigLoadFailureScreen(Exception exception, Runnable runnable) {
        this(exception, runnable, 0);
    }

    public BYGConfigLoadFailureScreen(Exception exception, Runnable runnable, int validationCountDown) {
        super(new TextComponent("BYG Config loading failed: "));
        this.exception = exception;
        this.runnable = runnable;
        this.validationCountDown = validationCountDown;
    }

    @Override
    public void tick() {
        if (validationCountDown > 0) {
            validationCountDown--;
            if (validateConfigs != null) {
                if (validationCountDown == 0) {
                    this.validateConfigs.setMessage(this.validateConfigsMessage);
                    this.validateConfigs.active = true;
                } else {
                    this.validateConfigs.setMessage(new TextComponent(Integer.toString((int) Math.ceil((double) this.validationCountDown / 20))));
                    this.validateConfigs.active = false;
                }
            }
        }
        super.tick();
    }

    @Override
    protected void init() {
        super.init();
        this.errorBoxTop = this.height / 4 - 25;
        int errorBoxLength = (int) Math.ceil(this.width / 1.3);

        StringWriter stringWriter = new StringWriter();
        this.exception.printStackTrace(new PrintWriter(stringWriter));

        int errorBoxBottom = errorBoxTop + (this.height / 2);
        this.text = new ScrollableText(new TextComponent("\n" + stringWriter.toString()), errorBoxLength, errorBoxTop, errorBoxTop, errorBoxBottom, FastColor.ARGB32.color(255, 255, 255, 255), true, true);
        this.text.setLeftPos(this.width / 2 - (errorBoxLength / 2));
        this.addRenderableWidget(this.text);

        int buttonWidth = (int) Math.ceil(errorBoxLength / 3.5);
        int buttonHeight = 20;
        int buttonY = errorBoxBottom + 10 + (buttonHeight / 2);
        this.validateConfigs = new Button((errorBoxLength / 3) - buttonWidth / 2, buttonY, buttonWidth, buttonHeight, new TextComponent("Validate Configs"), button -> {
            String loadAllConfigs = BYGConfigHandler.loadAllConfigs(true, false);
            try {
                if (!loadAllConfigs.isEmpty()) {
                    throw new IllegalStateException(loadAllConfigs);
                } else {
                    this.runnable.run();
                }
            } catch (Exception e) {
                e.printStackTrace();
                this.minecraft.setScreen(new BYGConfigLoadFailureScreen(e, this.runnable, VALIDATION_COOLDOWN));
            }
        }, (button, poseStack, mouseX, mouseZ) -> renderTooltip(poseStack, new TextComponent("Load configs again, if they work, you will continue to load into your world."), mouseX, mouseZ));
        if (validationCountDown > 0) {
            validateConfigs.active = false;
        }
        this.addRenderableWidget(new Button(((errorBoxLength / 3) * 2) - buttonWidth / 2, buttonY, buttonWidth, buttonHeight, new TextComponent("Config Directory"),
                button -> ((ScreenAccess) this).byg_invokeOpenLink(ModPlatform.INSTANCE.configPath().toUri()),
                (button, poseStack, mouseX, mouseZ) -> renderTooltip(poseStack, new TextComponent("Open config directory"), mouseX, mouseZ)
        ));

        this.addRenderableWidget(new Button(errorBoxLength - (buttonWidth / 2), buttonY, buttonWidth, buttonHeight, new TextComponent("Recreate configs"), button -> {
            UpdateConfigsCommand.backupConfigs(new ConfigVersionTracker(BYGConstants.CONFIG_VERSION));
            KillClient.kill();
        }, (button, poseStack, mouseX, mouseZ) -> renderTooltip(poseStack, new TextComponent("Backup & recreate your configs, then restart minecraft."), mouseX, mouseZ)));


        this.addRenderableWidget(validateConfigs);
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(poseStack);
        super.render(poseStack, mouseX, mouseY, partialTick);
        int width = this.minecraft.font.width(this.getTitle());
        this.minecraft.font.draw(poseStack, this.getTitle(), ((float) this.width / 2) - ((float) width / 2), this.errorBoxTop - 10, FastColor.ARGB32.color(255, 255, 0, 0));
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(null);
        super.onClose();
    }
}