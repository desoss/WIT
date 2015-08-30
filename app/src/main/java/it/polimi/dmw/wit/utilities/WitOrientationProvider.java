package it.polimi.dmw.wit.utilities;

import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

/**
 * Classe per gestire il sensore di orientazione.
 * Ho semplicemente cambiato i nomi alle variabili
 * per renderle più descrittive.
 *
 * Created by sivam on 1/17/15.
 */
public class WitOrientationProvider implements SensorEventListener {

    // TODO inserire valore medio dei sensori

    /**
     * Sensori e gestori
     */
    private SensorManager sensorManager;
    private Sensor magnetometer;
    private Sensor accelerometer;

    private float currentOrientation;

    /**
     * Vettori, matrici e compagnia bella
     */
    private float[] currentAcceleration = new float[3];
    private float[] currentMagnetVector = new float[3];
    private boolean hasAcceleration = false;
    private boolean hasMagnetVector = false;
    private float[] rotationMatrix = new float[9];
    private float[] orientationVector = new float[3];
    private AverageAngle averageValues;


    private class AverageAngle
    {
        private double[] values;
        private int currentIndex;
        private int numberOfFrames;
        private boolean isFull;
        private double averageValue = 0;

        public AverageAngle(int frames)
        {
            this.numberOfFrames = frames;
            this.currentIndex = 0;
            this.values = new double[frames];
        }

        public void putValue(double d)
        {
            values[currentIndex] = d; // numero di valori con cui fare la media
            if (currentIndex == numberOfFrames - 1) {
                currentIndex = 0;
                isFull = true;
            } else {
                currentIndex++;
            }
            updateAverageValue();
        }

        public double getAverage()
        {
            return this.averageValue;
        }

        private void updateAverageValue()
        {
            int numberOfElementsToConsider = numberOfFrames;
            if (!isFull) {
                numberOfElementsToConsider = currentIndex + 1;
            }

            if (numberOfElementsToConsider == 1) {
                this.averageValue = values[0];
                return;
            }

            // Formula: http://en.wikipedia.org/wiki/Circular_mean
            double sumSin = 0.0;
            double sumCos = 0.0;
            for (int i = 0; i < numberOfElementsToConsider; i++) {
                double v = values[i];
                sumSin += Math.sin(v);
                sumCos += Math.cos(v);
            }
            this.averageValue = Math.atan2(sumSin, sumCos);
        }
    }

    /**
     * Costruttore, riceve il gestore dei sensori del sistema
     *
     * @param senMan
     */
    public WitOrientationProvider(SensorManager senMan) {
        sensorManager = senMan;
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        averageValues = new AverageAngle(10);
    }

    /**
     * Inizia a cercare l'orientazione
     */
    public void startGettingOrientation() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * Smette di cercare l'orientazione
     */
    public void stopGettingOrientation() {
        sensorManager.unregisterListener(this, accelerometer);
        sensorManager.unregisterListener(this, magnetometer);
    }

    /**
     * Returns the angle with respect to the NORTH direction
     * in radians in range [-pi,+pi]
     *
     * NORTH = 0
     * EAST = pi/2
     * WEST = -pi/2
     * SUD = +pi, -pi
     *
     * Corregge l'angolo con la declinazione magnetica ottenuta
     * dalla Location
     *
     * @return
     */
    public double getOrientation(Location location) {
        GeomagneticField geoField = new GeomagneticField(
                (float) location.getLatitude(),
                (float) location.getLongitude(),
                (float) location.getAltitude(),
                System.currentTimeMillis());
        Log.d("OrientationProvider aV:",""+averageValues.getAverage());
        Log.d("Orientation provider d:", ""+Math.toRadians(geoField.getDeclination()));
        return averageValues.getAverage() + Math.toRadians(geoField.getDeclination());
    }

    /**
     * Metodo che viene chiamato quando ci sono dati dal sensore.
     * Con un pò di magia trova l'angolo rispetto alla direzione nord.
     * L'angolo viene salvato in currentOrientation.
     *
     * @param event
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == accelerometer) {
            System.arraycopy(event.values, 0, currentAcceleration, 0, event.values.length);
            hasAcceleration = true;
        } else if (event.sensor == magnetometer) {
            System.arraycopy(event.values, 0, currentMagnetVector, 0, event.values.length);
            hasMagnetVector = true;
        }

        if (hasAcceleration && hasMagnetVector) {
            SensorManager.getRotationMatrix(rotationMatrix, null, currentAcceleration, currentMagnetVector);
            SensorManager.getOrientation(rotationMatrix, orientationVector);
            averageValues.putValue(orientationVector[0]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
