package com.airhockey.android;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;

import com.airhockey.android.objects.Mallet;
import com.airhockey.android.objects.Puck;
import com.airhockey.android.objects.Table;
import com.airhockey.android.programs.ColorShaderProgram;
import com.airhockey.android.programs.TextureShaderProgram;
import com.airhockey.android.util.Geometry;
import com.airhockey.android.util.MatrixHelper;
import com.airhockey.android.util.TextureHelper;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.glClear;
import static android.opengl.GLES20.glClearColor;
import static android.opengl.GLES20.glViewport;
import static android.opengl.Matrix.invertM;
import static android.opengl.Matrix.multiplyMM;
import static android.opengl.Matrix.multiplyMV;
import static android.opengl.Matrix.rotateM;
import static android.opengl.Matrix.setIdentityM;
import static android.opengl.Matrix.setLookAtM;
import static android.opengl.Matrix.translateM;


public class AirHockeyRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = "AirHockeyRenderer";
    private final Context context;
    private final float[] projectionMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] viewProjectionMatrix = new float[16];
    private final float[] modelViewProjectMatrix = new float[16];
    private final float[] invertedViewProjectionMatrix = new float[16];

    private final float leftBound = -0.5f;
    private final float rightBound = 0.5f;
    private final float farBound = -0.8f;
    private final float nearBound = 0.8f;

    private Table table;
    private Mallet mallet;
    private Puck puck;


    private TextureShaderProgram textureProgram;
    private ColorShaderProgram colorProgram;

    private int texture;
    private boolean malletPressed = false;
    private Geometry.Point blueMalletPosition;
    private Geometry.Point previousBlueMalletPosition;
    private Geometry.Point puckPosition;
    private Geometry.Vector puckVector;


    public AirHockeyRenderer(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        table = new Table();
        mallet = new Mallet(0.08f, 0.15f, 32);
        puck = new Puck(0.06f, 0.02f, 32);
        blueMalletPosition = new Geometry.Point(0f, mallet.height / 2f, 0.4f);

        puckPosition = new Geometry.Point(0f, puck.height / 2f, 0f);
        puckVector = new Geometry.Vector(0f, 0f, 0f);

        textureProgram = new TextureShaderProgram(context);
        colorProgram = new ColorShaderProgram(context);

        texture = TextureHelper.loadTexture(context, R.drawable.air_hockey_surface);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        glViewport(0, 0, width, height);
        MatrixHelper.perspectiveM(projectionMatrix, 45, (float) width / (float) height, 1f, 10f);

        setLookAtM(viewMatrix, 0, 0f, 1.2f, 2.2f, 0f, 0f, 0f, 0f, 1f, 0f);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        glClear(GL_COLOR_BUFFER_BIT);

        puckPosition = puckPosition.translate(puckVector);

        if (puckPosition.x < leftBound + puck.radius || puckPosition.x > rightBound - puck.radius) {
            puckVector = new Geometry.Vector(-puckVector.x, puckVector.y, puckVector.z);
            puckVector = puckVector.scale(0.9f);
        }

        if (puckPosition.z < farBound + puck.radius || puckPosition.z > nearBound - puck.radius) {
            puckVector = new Geometry.Vector(puckVector.x, puckVector.y, -puckVector.z);
            puckVector = puckVector.scale(0.9f);
        }

        puckPosition = new Geometry.Point(clamp(puckPosition.x, leftBound + puck.radius, rightBound - puck.radius), puckPosition.y, clamp(puckPosition.z, farBound + puck.radius, nearBound - puck.radius));

        puckVector = puckVector.scale(0.99f);

        // Multiply the view and projection matrices together.
        multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
        invertM(invertedViewProjectionMatrix, 0, viewProjectionMatrix, 0);

        positionTableInScene();
        textureProgram.useProgram();
        textureProgram.setUniforms(modelViewProjectMatrix, texture);
        table.bindData(textureProgram);
        table.draw();

        positionObjectInScene(0f, mallet.height / 2f, -0.4f);
        colorProgram.useProgram();
        colorProgram.setUniforms(modelViewProjectMatrix, 1f, 0f, 0f);
        mallet.bindData(colorProgram);
        mallet.draw();

        positionObjectInScene(blueMalletPosition.x, blueMalletPosition.y, blueMalletPosition.z);
        colorProgram.setUniforms(modelViewProjectMatrix, 0f, 0f, 1f);
        mallet.draw();

        positionObjectInScene(puckPosition.x, puckPosition.y, puckPosition.z);
        colorProgram.setUniforms(modelViewProjectMatrix, 0.8f, 0.8f, 1.0f);
        puck.bindData(colorProgram);
        puck.draw();

    }

    private void positionObjectInScene(float x, float y, float z) {
        setIdentityM(modelMatrix, 0);
        translateM(modelMatrix, 0, x, y, z);
        multiplyMM(modelViewProjectMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0);

    }

    private void positionTableInScene() {
        setIdentityM(modelMatrix, 0);
        rotateM(modelMatrix, 0, -90f, 1f, 0f, 0f);
        multiplyMM(modelViewProjectMatrix, 0, viewProjectionMatrix, 0, modelMatrix, 0);
    }

    public void handleTouchPress(float normalizedX, float normalizedY) {
        Log.d(TAG, "handleTouchPress: " + normalizedX + " ," + normalizedY);
        Geometry.Ray ray = convertNormalized2DPointToRay(normalizedX, normalizedY);
        Geometry.Sphere malletBoundingSphere = new Geometry.Sphere(new Geometry.Point(blueMalletPosition.x, blueMalletPosition.y, blueMalletPosition.z), mallet.height / 2f);
        malletPressed = Geometry.intersects(malletBoundingSphere, ray);
        Log.e(TAG, "malletPressed: " + malletPressed);
    }

    private Geometry.Ray convertNormalized2DPointToRay(float normalizedX, float normalizedY) {
        final float[] nearPointNdc = {normalizedX, normalizedY, -1, 1};
        final float[] farPointNdc = {normalizedX, normalizedY, 1, 1};

        final float[] nearPointWorld = new float[4];
        final float[] farPointWorld = new float[4];

        multiplyMV(nearPointWorld, 0, invertedViewProjectionMatrix, 0, nearPointNdc, 0);
        multiplyMV(farPointWorld, 0, invertedViewProjectionMatrix, 0, farPointNdc, 0);
        divideByW(nearPointWorld);
        divideByW(farPointWorld);

        Geometry.Point nearPointRay = new Geometry.Point(nearPointWorld[0], nearPointWorld[1], nearPointWorld[2]);
        Geometry.Point farPointRay = new Geometry.Point(farPointWorld[0], farPointWorld[1], farPointWorld[2]);

        return new Geometry.Ray(nearPointRay, Geometry.vectorBetween(nearPointRay, farPointRay));
    }

    private void divideByW(float[] vector) {
        vector[0] /= vector[3];
        vector[1] /= vector[3];
        vector[2] /= vector[3];
    }

    public void handleTouchDrag(float normalizedX, float normalizedY) {
        Log.d(TAG, "handleTouchDrag: " + normalizedX + " ," + normalizedY);
        if (malletPressed) {
            previousBlueMalletPosition = blueMalletPosition;

            Geometry.Ray ray = convertNormalized2DPointToRay(normalizedX, normalizedY);
            Geometry.Plane plane = new Geometry.Plane(new Geometry.Point(0, 0, 0), new Geometry.Vector(0, 1, 0));
            Geometry.Point touchedPoint = Geometry.intersectionPoint(ray, plane);
            blueMalletPosition = new Geometry.Point(clamp(touchedPoint.x, leftBound + mallet.radius, rightBound - mallet.radius), mallet.height / 2f, clamp(touchedPoint.z, 0f + mallet.radius, nearBound - mallet.radius));

            float distance = Geometry.vectorBetween(blueMalletPosition, puckPosition).length();
            if (distance < puck.radius + mallet.radius) {
                puckVector = Geometry.vectorBetween(previousBlueMalletPosition, blueMalletPosition);
            }
        }
    }

    private float clamp(float value, float min, float max) {
        return Math.min(max, Math.max(min, value));
    }
}
