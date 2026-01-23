package com.gauntletlocker;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import net.runelite.api.FontID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.chatbox.ChatboxInput;
import net.runelite.client.game.chatbox.ChatboxPanelManager;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.MouseListener;

public class GrimReaperDialogueInput extends ChatboxInput implements KeyListener, MouseListener
{
    private static final int MODELTYPE_NPC_CHATHEAD = 2;

    private static final int COL_WHITE = 0xFFFFFF;
    private static final int COL_YELLOW = 0xFFFF00;
    private static final int COL_BLUE = 0x0000FF;

    private final ClientThread clientThread;
    private final ChatboxPanelManager chatboxPanelManager;

    private final int npcId;
    private final int chatheadAnimId;
    private final String npcName;
    private final String text;

    private Widget container;
    private Widget continueWidget;

    private boolean closeRequested = false;
    private boolean closed = false;

    public GrimReaperDialogueInput(
            ClientThread clientThread,
            ChatboxPanelManager chatboxPanelManager,
            int npcId,
            int chatheadAnimId,
            String npcName,
            String text
    )
    {
        super();
        this.clientThread = clientThread;
        this.chatboxPanelManager = chatboxPanelManager;
        this.npcId = npcId;
        this.chatheadAnimId = chatheadAnimId;
        this.npcName = npcName;
        this.text = text;
    }

    public boolean isCloseRequested()
    {
        return closeRequested;
    }

    public boolean isClosed()
    {
        return closed;
    }

    @Override
    protected void open()
    {
        closed = false;
        closeRequested = false;

        container = chatboxPanelManager.getContainerWidget();
        if (container == null)
        {
            return;
        }

        container.deleteAllChildren();

        final int w = Math.max(container.getWidth(), container.getOriginalWidth());
        final int h = Math.max(container.getHeight(), container.getOriginalHeight());

        // Chathead
        Widget head = container.createChild(-1, WidgetType.MODEL);
        head.setOriginalX(10);
        head.setOriginalY(8);
        head.setOriginalWidth(64);
        head.setOriginalHeight(64);
        head.setModelType(MODELTYPE_NPC_CHATHEAD);
        head.setModelId(npcId);
        head.setAnimationId(chatheadAnimId);
        head.setModelZoom(900);
        head.setRotationX(160);
        head.setRotationY(0);
        head.setRotationZ(0);

        // Name
        Widget nameW = container.createChild(-1, WidgetType.TEXT);
        nameW.setOriginalX(84);
        nameW.setOriginalY(8);
        nameW.setOriginalWidth(Math.max(0, w - 90));
        nameW.setOriginalHeight(16);
        nameW.setFontId(FontID.PLAIN_12);
        nameW.setTextShadowed(true);
        nameW.setTextColor(COL_YELLOW);
        nameW.setText(npcName);

        // Dialogue text
        Widget textW = container.createChild(-1, WidgetType.TEXT);
        textW.setOriginalX(84);
        textW.setOriginalY(24);
        textW.setOriginalWidth(Math.max(0, w - 90));
        textW.setOriginalHeight(40);
        textW.setFontId(FontID.PLAIN_12);
        textW.setTextShadowed(true);
        textW.setTextColor(COL_WHITE);
        textW.setText(text);

        // Continue hint
        continueWidget = container.createChild(-1, WidgetType.TEXT);
        continueWidget.setOriginalX(84);
        continueWidget.setOriginalY(Math.max(0, h - 18));
        continueWidget.setOriginalWidth(Math.max(0, w - 90));
        continueWidget.setOriginalHeight(16);
        continueWidget.setFontId(FontID.PLAIN_12);
        continueWidget.setTextShadowed(true);
        continueWidget.setTextColor(COL_BLUE);
        continueWidget.setText("Press space to continue");

        container.revalidate(); // critical
    }

    @Override
    protected void close()
    {
        closed = true;
        if (container != null)
        {
            container.deleteAllChildren();
            container = null;
        }
    }

    private void requestCloseNextTickFromSpace()
    {
        if (closeRequested)
        {
            return;
        }

        closeRequested = true;

        if (continueWidget != null)
        {
            continueWidget.setTextColor(COL_WHITE);
            continueWidget.revalidate();
        }
    }

    // Space: set prompt white; plugin closes next GameTick
    @Override
    public void keyPressed(KeyEvent e)
    {
        if (e.getKeyCode() == KeyEvent.VK_SPACE)
        {
            e.consume();
            clientThread.invokeLater(this::requestCloseNextTickFromSpace);
        }
    }

    @Override public void keyReleased(KeyEvent e) {}
    @Override public void keyTyped(KeyEvent e) {}

    // Click: close immediately
    @Override
    public MouseEvent mousePressed(MouseEvent e)
    {
        e.consume();
        clientThread.invokeLater(chatboxPanelManager::close);
        return e;
    }

    @Override public MouseEvent mouseClicked(MouseEvent e) { return e; }
    @Override public MouseEvent mouseReleased(MouseEvent e) { return e; }
    @Override public MouseEvent mouseEntered(MouseEvent e) { return e; }
    @Override public MouseEvent mouseExited(MouseEvent e) { return e; }
    @Override public MouseEvent mouseDragged(MouseEvent e) { return e; }
    @Override public MouseEvent mouseMoved(MouseEvent e) { return e; }
}
