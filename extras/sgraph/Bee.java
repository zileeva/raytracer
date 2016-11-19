package sgraph;

import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by yuliazileeva on 10/24/16.
 */
public class Bee {

    HashMap<String, INode> nodes;
    INode bee, rightwing, leftwing;
    Integer sign;

    /**
     * Bird Model
     * Transform nodes - bird, right and left wings, right and left ulnas
     * @param map nodes
     * @param order bird order
     */
    public Bee(Map<String,INode> map, String order) {

        nodes = (HashMap<String, INode>) map;
        rightwing = nodes.get(order + "-rightwing");
        leftwing = nodes.get(order + "-leftwing");
        bee = nodes.get(order + "-bee");

        if (Integer.parseInt(order) == 1) sign = 1;
        else sign = -1;

    }

    /**
     * Animates bird model
     * @param time
     */
    public void animate(float time) {

        animateModel(time);
        animateWings(time);

    }

    /**
     * Animates bird body to move along a pre-defined path
     * @param time
     */
    private void animateModel(float time) {
        bee.setAnimationTransform(
                new Matrix4f()
                        .rotate(0.01f * time, 0, 1, 0)
                        .translate(0, (float) (100 * Math.sin(time * 0.05)) + 400, sign * 250)
                        .rotate(sign * 90, 0, 1, 0)
        );
    }

    /**
     * Animates two wings and ulnas to move them up and down.
     * Calculates rotation angles for entire wing and ulna part
     * @param time
     */
    private void animateWings(float time) {
        Float wingsTime = time;
        Float angle;

        angle = (float) (0.5 * Math.sin(0.4 * wingsTime));

        animateWing(rightwing, angle);

        angle = (float) (0.5 * Math.sin(0.4 * -wingsTime));

        animateWing(leftwing, angle);
    }

    /**
     * Animates wing and ulna
     * @param wing
     * @param angle rotation angle for wing
     */
    private void animateWing(INode wing, float angle) {
        wing.setAnimationTransform(new Matrix4f().rotate(angle, 0, 0, 1));;
    }

}
