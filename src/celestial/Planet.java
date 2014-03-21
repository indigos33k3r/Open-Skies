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
 * Defines a planet. Planets in this simulation have infinite mass so they will
 * stay put.
 */
package celestial;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Image;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import jmeplanet.FractalDataSource;
import jmeplanet.PlanetAppState;
import jmeplanet.Utility;
import lib.astral.Parser.Term;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class Planet extends Celestial {

    private transient Texture2D tex;
    transient jmeplanet.Planet fractalPlanet;
    transient jmeplanet.Planet atmosphereShell;
    protected transient RigidBodyControl atmospherePhysics;
    private Term type;
    private int seed = 0;
    protected float radius;

    public Planet(Universe universe, String name, Term type, float radius) {
        super(Float.POSITIVE_INFINITY, universe);
        setName(name);
        this.type = type;
        this.radius = radius;
    }

    public void construct(AssetManager assets) {
        generateProceduralPlanet(assets);
        if (spatial != null) {
            //initializes the physics as a sphere
            SphereCollisionShape sphereShape = new SphereCollisionShape(radius);
            //setup dynamic physics
            physics = new RigidBodyControl(sphereShape, getMass());
            //add physics to mesh
            spatial.addControl(physics);
            if (atmosphereShell != null) {
                atmospherePhysics = new RigidBodyControl(sphereShape, getMass());
                atmosphereShell.addControl(atmospherePhysics);
                //avoid collissions
                atmospherePhysics.setKinematic(false);
                atmosphereShell.getControl(RigidBodyControl.class).setCollideWithGroups(PhysicsCollisionObject.COLLISION_GROUP_NONE);
            }
        }
    }

    public void deconstruct() {
        setTex(null);
        mat = null;
        spatial = null;
        physics = null;
        atmosphereShell = null;
        atmospherePhysics = null;
    }

    private void generateProceduralPlanet(AssetManager assets) {
        //setup seeded rng
        Random sRand = new Random(seed);
        //get group and palette
        String group = type.getValue("group");
        String palette = type.getValue("palette");
        //split based on planet group
        if (group.equals("rock")) {
            //determine height scale
            float heightScale = (sRand.nextFloat()*0.02f)+0.01f; //1% to 3%
            if (palette.equals("Earth")) {
                // Add planet
                FractalDataSource planetDataSource = new FractalDataSource(seed);
                planetDataSource.setHeightScale(heightScale * radius);
                fractalPlanet = Utility.createEarthLikePlanet(assets, radius, null, planetDataSource);
                spatial = fractalPlanet;
            } else if (palette.equals("Barren")) {
                FractalDataSource moonDataSource = new FractalDataSource(seed);
                moonDataSource.setHeightScale(heightScale * radius);
                fractalPlanet = Utility.createMoonLikePlanet(assets, radius, moonDataSource);
                spatial = fractalPlanet;
            } else if (palette.equals("Lava")) {
                // Add planet
                FractalDataSource planetDataSource = new FractalDataSource(seed);
                planetDataSource.setHeightScale(heightScale * radius);
                fractalPlanet = Utility.createChthonianPlanet(assets, radius, null, planetDataSource, seed);
                spatial = fractalPlanet;
            } else if (palette.equals("Mars")) {
                //determine water presence
                boolean hasWater = Boolean.parseBoolean(type.getValue("hasWater"));
                // Add planet
                FractalDataSource planetDataSource = new FractalDataSource(seed);
                planetDataSource.setHeightScale(heightScale * radius);
                fractalPlanet = Utility.createMarsLikePlanet(assets, radius, null, planetDataSource, hasWater, seed);
                spatial = fractalPlanet;
            }
        } else if (group.equals("gas")) {
            Color airColor = Color.WHITE;
            if (palette.equals("BandedGas")) {
                //create a canvas
                BufferedImage buff = new BufferedImage(2048, 1024, BufferedImage.TYPE_INT_RGB);
                Graphics2D gfx = (Graphics2D) buff.getGraphics();
                //draw debug texture
                gfx.setColor(new Color(0, 0, 0, 0));
                gfx.fillRect(0, 0, buff.getWidth(), buff.getHeight());
                /*
                 * Setup the sphere since we aren't using the procedural planet generator
                 * supplied in the jmeplanet package
                 */
                //create geometry
                Sphere objectSphere = new Sphere(256, 256, radius);
                objectSphere.setTextureMode(Sphere.TextureMode.Projected);
                spatial = new Geometry("Planet", objectSphere);
                //retrieve texture
                mat = new Material(assets, "Common/MatDefs/Light/Lighting.j3md");
                mat.setFloat("Shininess", 0.32f);
                mat.setBoolean("UseMaterialColors", false);
                mat.setColor("Ambient", ColorRGBA.Black);
                mat.setColor("Specular", ColorRGBA.White);
                mat.setColor("Diffuse", ColorRGBA.White);
                mat.getAdditionalRenderState().setBlendMode(BlendMode.Off);
                /*
                 * My gas giants are conservative. They have a color and brightness
                 * which is held constant while bands are drawn varying the saturation.
                 * 
                 * Two passes are made. The first draws primary bands, which define the
                 * overall look. The second does secondary bands which help de-alias
                 * the planet.
                 */
                //determine band count
                int bands = sRand.nextInt(75) + 25;
                int height = (buff.getHeight() / bands);
                //pick sat and val
                float sat = sRand.nextFloat();
                float value = sRand.nextFloat();
                if (value < 0.45f) {
                    value = 0.45f;
                }
                //pick a hue
                float hue = sRand.nextFloat();
                //draw a baseplate
                airColor = new Color(Color.HSBtoRGB(hue, sat, value));
                gfx.setColor(airColor);
                gfx.fillRect(0, 0, buff.getWidth(), buff.getHeight());
                //pass 1, big bands
                for (int a = 0; a < bands / 2; a++) {
                    //vary saturation
                    sat = sRand.nextFloat();
                    //draw a band
                    Color raw = new Color(Color.HSBtoRGB(hue, sat, value));
                    Color col = new Color(raw.getRed(), raw.getGreen(), raw.getBlue(), 64);
                    gfx.setColor(col);
                    gfx.fillRect(0, height / 2 * (a), buff.getWidth(), height);
                }
                //pass 2, small secondary bands
                for (int a = 0; a < bands * 4; a++) {
                    //vary saturation
                    sat = sRand.nextFloat();
                    //draw a band
                    Color raw = new Color(Color.HSBtoRGB(hue, sat, value));
                    Color col = new Color(raw.getRed(), raw.getGreen(), raw.getBlue(), 16);
                    gfx.setColor(col);
                    gfx.fillRect(0, height / 4 * (a), buff.getWidth(), height);
                }
                //map to material
                Image load = new AWTLoader().load(buff, true);
                setTex(new Texture2D(load));
                mat.setTexture("DiffuseMap", getTex());
                spatial.setMaterial(mat);
            }
            //rotate
            setRotation(getRotation().fromAngles(FastMath.PI / 2, 0, 0));
            //add an atmosphere
            FractalDataSource planetDataSource = new FractalDataSource(seed);
            planetDataSource.setHeightScale(0.015f * radius);
            //generate color
            float colR = (float) airColor.getRed() / 255.0f;
            float colG = (float) airColor.getGreen() / 255.0f;
            float colB = (float) airColor.getBlue() / 255.0f;
            ColorRGBA atmoColor = new ColorRGBA(colR, colG, colB, 0.5f);
            //generate shell
            atmosphereShell = Utility.createAtmosphereShell(assets, radius * 1.01f, planetDataSource, atmoColor);
        }
    }

    protected void alive() {
        if (physics != null) {
            if (spatial != null) {
                //planets do not move
                physics.setPhysicsLocation(getLocation());
                physics.setPhysicsRotation(getRotation());
                spatial.setLocalRotation(getRotation());
                if (atmosphereShell != null) {
                    atmospherePhysics.setPhysicsLocation(getLocation());
                    atmospherePhysics.setPhysicsRotation(getRotation());
                    atmosphereShell.setLocalRotation(getRotation());
                }
            }
        }
    }

    public void attach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        if (spatial != null) {
            node.attachChild(spatial);
            physics.getPhysicsSpace().add(spatial);
            if (fractalPlanet != null) {
                planetAppState.addPlanet(fractalPlanet);
            }
            if (atmosphereShell != null) {
                node.attachChild(atmosphereShell);
                physics.getPhysicsSpace().add(atmospherePhysics);
                planetAppState.addPlanet(atmosphereShell);
            }
        }
    }

    public void detach(Node node, BulletAppState physics, PlanetAppState planetAppState) {
        node.detachChild(spatial);
        physics.getPhysicsSpace().remove(spatial);
        if (fractalPlanet != null) {
            planetAppState.removePlanet(fractalPlanet);
        }
        if (atmosphereShell != null) {
            node.detachChild(atmosphereShell);
            physics.getPhysicsSpace().remove(atmospherePhysics);
            planetAppState.removePlanet(atmosphereShell);
        }
    }

    public float getRadius() {
        return radius;
    }

    public void setRadius(float radius) {
        this.radius = radius;
    }

    public Term getType() {
        return type;
    }

    public void setType(Term type) {
        this.type = type;
    }

    public Texture2D getTex() {
        return tex;
    }

    public void setTex(Texture2D tex) {
        this.tex = tex;
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(int seed) {
        this.seed = seed;
    }
}
