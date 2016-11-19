package sgraph;

import org.joml.Matrix4f;

import java.util.*;

/**
 * Created by yuliazileeva on 10/24/16.
 */
public class Bird {

    HashMap<String, INode> nodes;
    INode bird, rightwing, leftwing, rightulna, leftulna, beak;
    Integer sign;

    /**
     * Bird Model
     * Transform nodes - bird, right and left wings, right and left ulnas
     * @param map nodes
     * @param order bird order
     */
    public Bird(Map<String,INode> map, String order) {

        nodes = (HashMap<String, INode>) map;
        rightwing = nodes.get(order + "-rightwing");
        leftwing = nodes.get(order + "-leftwing");
        bird = nodes.get(order + "-bird");
        rightulna = nodes.get(order + "-rightlowerarm");
        leftulna = nodes.get(order + "-leftlowerarm");
        beak = nodes.get(order + "-beak");

        if (Integer.parseInt(order) == 1) sign = 1;
        else sign = -1;

    }

    public INode getBirdNode() {
        return bird;
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
        bird.setAnimationTransform(
                new Matrix4f()
                        .rotate((float) Math.toRadians(sign * 20), 0, 0, 1)
                        .rotate(0.01f * time, 0, 1, 0)
                        .translate(0, 0, sign * 350)
                        .rotate(sign * 90, 0, 1, 0)
        );
    }

    /**
     * Animates two wings and ulnas to move them up and down.
     * Calculates rotation angles for entire wing and ulna part
     * @param time
     */
    private void animateWings(float time) {
        Float wingsTime = (2 * time);
        Float angle = (float) Math.toRadians(wingsTime);
        Float elbowAngle = (float) Math.toRadians(-wingsTime);

        angle = (float) (0.5 * Math.sin (0.1 * wingsTime));
        elbowAngle = (float) (0.3 * Math.sin (0.1 * wingsTime));

        animateWing(rightwing, rightulna, angle, elbowAngle);

        angle = (float) (0.5 * Math.sin (0.1 * -wingsTime));
        elbowAngle = (float) (0.3 * Math.sin (0.1 * -wingsTime));

        animateWing(leftwing, leftulna, angle, elbowAngle);
    }

    /**
     * Animates wing and ulna
     * @param wing
     * @param ulna
     * @param angle rotation angle for wing
     * @param elbowAngle rotation angle for ulna
     */
    private void animateWing(INode wing, INode ulna, float angle, float elbowAngle) {
        wing.setAnimationTransform(new Matrix4f().rotate(angle, 0, 0, 1));
        ulna.setAnimationTransform(new Matrix4f().rotate(elbowAngle, 0, 0, 1));
    }

}
