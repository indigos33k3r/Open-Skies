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
 * Now for some meat. This class represents a turret.
 */
package cargo;

import celestial.Projectile;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import entity.Entity;
import java.util.ArrayList;
import lib.astral.Parser;

/**
 *
 * @author Nathan Wiehoff
 */
public class Weapon extends Equipment {

    int width;
    int height;
    //weapon properties
    private float shieldDamage;
    private float hullDamage;
    private float speed;
    //weapon graphics
    private float size = 1;
    private ColorRGBA startColor = ColorRGBA.Red;
    private ColorRGBA endColor = ColorRGBA.Blue;
    private Vector3f pVel = Vector3f.UNIT_XYZ;
    private float highLife = 1;
    private float lowLife = 0.1f;
    private int numParticles = 10;
    private float variation = 0.75f;
    private String texture = "Effects/Trail/point.png";
    private float emitterRate = 10;

    public Weapon(String name) {
        super(name);
        init();
    }

    private void init() {
        //get weapon stuff now
        Parser parse = new Parser("WEAPONS.txt");
        ArrayList<Parser.Term> terms = parse.getTermsOfType("Weapon");
        Parser.Term relevant = null;
        for (int a = 0; a < terms.size(); a++) {
            String termName = terms.get(a).getValue("name");
            if (termName.matches(getName())) {
                //get the stats we want
                relevant = terms.get(a);
                //and end
                break;
            }
        }
        if (relevant != null) {
            setName(relevant.getValue("name"));
            setType(relevant.getValue("type"));
            setMass(Float.parseFloat(relevant.getValue("mass")));
            setShieldDamage(Float.parseFloat(relevant.getValue("shieldDamage")));
            setHullDamage(Float.parseFloat(relevant.getValue("hullDamage")));
            setRange(Float.parseFloat(relevant.getValue("range")));
            setSpeed(Float.parseFloat(relevant.getValue("speed")));
            setCoolDown(Float.parseFloat(relevant.getValue("refire")));
            setSize(Float.parseFloat(relevant.getValue("size")));
            {
                //start color
                String rawStart = relevant.getValue("startColor");
                String[] arr = rawStart.split(",");
                float r = Float.parseFloat(arr[0]);
                float g = Float.parseFloat(arr[1]);
                float b = Float.parseFloat(arr[2]);
                float a = Float.parseFloat(arr[3]);
                ColorRGBA col = new ColorRGBA(r, g, b, a);
                startColor = col;
            }
            {
                //end color
                String rawStart = relevant.getValue("endColor");
                String[] arr = rawStart.split(",");
                float r = Float.parseFloat(arr[0]);
                float g = Float.parseFloat(arr[1]);
                float b = Float.parseFloat(arr[2]);
                float a = Float.parseFloat(arr[3]);
                ColorRGBA col = new ColorRGBA(r, g, b, a);
                endColor = col;
            }
            {
                //pVel : The velocity of the particles in their local space
                String rawVel = relevant.getValue("pVel");
                String[] arr = rawVel.split(",");
                float x = Float.parseFloat(arr[0]);
                float y = Float.parseFloat(arr[1]);
                float z = Float.parseFloat(arr[2]);
                Vector3f v = new Vector3f(x, y, z);
                pVel = v;
            }
            setHighLife(Float.parseFloat(relevant.getValue("highLife")));
            setLowLife(Float.parseFloat(relevant.getValue("lowLife")));
            setNumParticles(Integer.parseInt(relevant.getValue("numParticles")));
            setVariation(Float.parseFloat(relevant.getValue("variation")));
            setTexture(relevant.getValue("texture"));
            setEmitterRate(Float.parseFloat(relevant.getValue("emitterRate")));
        } else {
            System.out.println("Error: The item " + getName() + " does not exist in WEAPONS.txt");
        }
    }

    @Override
    public void activate(Entity target) {
        if (getCoolDown() <= getActivationTimer() && enabled) {
            setActivationTimer(0); //restart cooldown
            //determine if OOS or not
            if (host.getCurrentSystem() == host.getCurrentSystem().getUniverse().getPlayerShip().getCurrentSystem()) {
                fire(target);
            } else {
                oosFire(target);
            }
        }
    }

    private void fire(Entity target) {
        /*
         * This is the one called in system. It uses the physics system and is
         * under the assumption that the host has been fully constructed. It
         * generates a projectile using precached values, assigns those values,
         * and finally drops the projectile into space.
         */
        if (enabled) {
            //generate projectile
            Projectile pro = new Projectile(host.getCurrentSystem().getUniverse(), getName());
            //store stats
            pro.setShieldDamage(shieldDamage);
            pro.setHullDamage(hullDamage);
            pro.setSpeed(speed);
            pro.setRange(range);
            //store graphics
            pro.setSize(size);
            pro.setHighLife(highLife);
            pro.setLowLife(lowLife);
            pro.setNumParticles(numParticles);
            pro.setVariation(variation);
            pro.setTexture(texture);
            pro.setEmitterRate(emitterRate);
            pro.setStartColor(startColor);
            pro.setEndColor(endColor);
            pro.setpVel(pVel);
            //determine world location and rotation
            Vector3f loc = getSocket().getNode().getWorldTranslation().add(host.getLinearVelocity().mult(tpf));
            Quaternion rot = getSocket().getNode().getWorldRotation();
            //interpolate velocity
            Vector3f vel = Vector3f.UNIT_Z.mult(-(speed));
            rot.multLocal(vel);
            vel = vel.add(host.getLinearVelocity());
            //store physics
            pro.setLocation(loc);
            pro.setRotation(rot);
            pro.setVelocity(vel);
            //store host
            pro.setHost(host);
            pro.setOrigin(socket);
            //put projectile in system
            host.getCurrentSystem().putEntityInSystem(pro);
        }
    }

    private void oosFire(Entity target) {
        if (enabled) {
            //TODO: DEAL DAMAGE TO TARGET DIRECTLY
        }
    }

    public float getShieldDamage() {
        return shieldDamage;
    }

    public void setShieldDamage(float shieldDamage) {
        this.shieldDamage = shieldDamage;
    }

    public float getHullDamage() {
        return hullDamage;
    }

    public void setHullDamage(float hullDamage) {
        this.hullDamage = hullDamage;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getSize() {
        return size;
    }

    public void setSize(float size) {
        this.size = size;
    }

    public ColorRGBA getStartColor() {
        return startColor;
    }

    public void setStartColor(ColorRGBA startColor) {
        this.startColor = startColor;
    }

    public ColorRGBA getEndColor() {
        return endColor;
    }

    public void setEndColor(ColorRGBA endColor) {
        this.endColor = endColor;
    }

    public Vector3f getpVel() {
        return pVel;
    }

    public void setpVel(Vector3f pVel) {
        this.pVel = pVel;
    }

    public float getHighLife() {
        return highLife;
    }

    public void setHighLife(float highLife) {
        this.highLife = highLife;
    }

    public float getLowLife() {
        return lowLife;
    }

    public void setLowLife(float lowLife) {
        this.lowLife = lowLife;
    }

    public int getNumParticles() {
        return numParticles;
    }

    public void setNumParticles(int numParticles) {
        this.numParticles = numParticles;
    }

    public float getVariation() {
        return variation;
    }

    public void setVariation(float variation) {
        this.variation = variation;
    }

    public String getTexture() {
        return texture;
    }

    public void setTexture(String texture) {
        this.texture = texture;
    }

    public float getEmitterRate() {
        return emitterRate;
    }

    public void setEmitterRate(float emitterRate) {
        this.emitterRate = emitterRate;
    }
}
