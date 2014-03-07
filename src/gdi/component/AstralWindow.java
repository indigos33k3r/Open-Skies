/*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

/*
 * This is a window. It stores components.
 */
package gdi.component;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 *
 * @author Nathan Wiehoff
 */
public class AstralWindow extends AstralComponent {

    protected Color backColor = Color.PINK;
    protected int order = 0;
    ArrayList<AstralComponent> components = new ArrayList<>();
    BufferedImage buffer;
    AssetManager assets;
    //quad
    Quad qd_background;
    Geometry geo_background;
    Material mat_background;
    Texture2D myTex;
    AWTLoader awtLoader;

    public AstralWindow(AssetManager assets, int width, int height) {
        super(width, height);
        this.assets = assets;
        createQuad();
        render(null);
    }

    private void createQuad() {
        qd_background = new Quad(getWidth(), getHeight());
        geo_background = new Geometry("Background", qd_background);
        mat_background = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        myTex = new Texture2D();
        awtLoader = new AWTLoader();
        buffer = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        myTex.setImage(awtLoader.load(buffer, false));
        mat_background.setTexture("ColorMap", myTex);
        geo_background.setMaterial(mat_background);
    }

    public void addComponent(AstralComponent component) {
        components.add(component);
    }

    public void removeComponent(AstralComponent component) {
        components.remove(component);
    }

    @Override
    public void periodicUpdate() {
        geo_background.setLocalTranslation(x, y, 0);
        for (int a = 0; a < components.size(); a++) {
            components.get(a).periodicUpdate();
        }
    }

    public void add(Node guiNode) {
        guiNode.attachChild(geo_background);
    }

    public void remove(Node guiNode) {
        guiNode.detachChild(geo_background);
    }

    @Override
    public final void render(Graphics f) {
        try {
            if (visible) {
                //get graphics
                Graphics2D s = (Graphics2D) buffer.getGraphics();
                //render the backdrop
                s.setColor(backColor);
                s.fillRect(0, 0, getWidth(), getHeight());
                //render components
                for (int a = 0; a < components.size(); a++) {
                    components.get(a).render(s);
                }
                //draw focus borders
                if (focused) {
                    s.setColor(getFocusColor());
                    s.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                }
                //flip
                AffineTransform tx = AffineTransform.getScaleInstance(1, -1);
                tx.translate(0, -buffer.getHeight(null));
                AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                buffer = op.filter(buffer, null);
                //push frame to quad
                myTex.setImage(awtLoader.load(buffer, false));
                mat_background.setTexture("ColorMap", myTex);
                geo_background.setMaterial(mat_background);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handleKeyPressedEvent(String ke) {
        if (visible) {
            for (int a = 0; a < components.size(); a++) {
                if (components.get(a).isFocused()) {
                    components.get(a).handleKeyPressedEvent(ke);
                }
            }
        }
    }

    @Override
    public void handleKeyReleasedEvent(String ke) {
        if (visible) {
            for (int a = 0; a < components.size(); a++) {
                if (components.get(a).isFocused()) {
                    components.get(a).handleKeyReleasedEvent(ke);
                }
            }
        }
    }

    @Override
    public void handleMousePressedEvent(String me, Vector3f mouseLoc) {
        if (visible) {
            Vector3f adjLoc = new Vector3f(mouseLoc.x - x, (int) (mouseLoc.y-(mouseLoc.z-getHeight())+y), 0);
            Rectangle mRect = new Rectangle((int)adjLoc.x, (int)adjLoc.y, 1, 1);
            for (int a = 0; a < components.size(); a++) {
                if (components.get(a).intersects(mRect)) {
                    components.get(a).setFocused(true);
                    components.get(a).handleMousePressedEvent(me, adjLoc);
                } else {
                    components.get(a).setFocused(false);
                }
            }
        }
    }

    @Override
    public void handleMouseReleasedEvent(String me, Vector3f mouseLoc) {
        Vector3f adjLoc = new Vector3f(mouseLoc.x - x, (int) (mouseLoc.y-(mouseLoc.z-getHeight())+y), 0);
        if (visible) {
            for (int a = 0; a < components.size(); a++) {
                if(components.get(a).isFocused()) {
                    components.get(a).handleMouseReleasedEvent(me, adjLoc);
                }
            }
        }
    }

    @Override
    public void handleMouseMovedEvent(MouseEvent me) {
        /**/
    }

    public Color getBackColor() {
        return backColor;
    }

    public void setBackColor(Color backColor) {
        this.backColor = backColor;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
