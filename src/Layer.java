import java.io.PrintWriter;
import java.util.Arrays;

public class Layer {
    private float[][] weights;
    private float[] bias;

    private float[] lastInput;
    private float[] lastOutput;

    public Layer(int inputSize, int outputSize) {
        weights = new float[outputSize][inputSize];
        bias = new float[outputSize];
        randomize();
    }

    public void randomize() {
        for (int r=0; r<weights.length; r++) { // [0,1) -> [-.5,.5) -> [-.1,.1)
            bias[r] = (float) (Math.random() - .5) / 5;
            for (int c=0; c<weights[0].length; c++) {
                weights[r][c] = (float) (Math.random() - .5) / 5;
            }
        }
    }
    public Layer(float[][] weights, float[] bias) {
        this.weights = weights;
        this.bias = bias;
    }

    public float[] forwardPass(float[] input) {
        if (input.length != weights[0].length)
            throw new RuntimeException("Triangle in square hole lol");
        lastInput = input.clone();
        lastOutput = new float[weights.length];
        for (int r=0; r<weights.length; r++) {
            float sum = bias[r];
            for (int c=0; c<weights[0].length; c++) {
                sum += weights[r][c] * input[c];
            }
            lastOutput[r] = (float)Math.tanh(sum);
        }
        return lastOutput;
    }
    public float[] backwardPropagate(float[] nextDelta, float[][] nextWeights, float lr) {
        float[] delta = new float[weights.length];

        // compute this layer's delta
        for (int i = 0; i < weights.length; i++) {
            float error = 0;
            for (int k = 0; k < nextDelta.length; k++) {
                error += nextWeights[k][i] * nextDelta[k];
            }
            // tanh derivative
            float deriv = 1 - lastOutput[i]*lastOutput[i];
            delta[i] = error * deriv;
        }

        // update weights
        for (int r = 0; r < weights.length; r++) {
            for (int c = 0; c < weights[0].length; c++) {
                weights[r][c] -= lr * delta[r] * lastInput[c];
            }
            // 3. update bias
            bias[r] -= lr * delta[r];
        }

        return delta;
    }

    public float[] backwardOutput(float[] target, float lr) {
        float[] delta = new float[weights.length];

        for (int i = 0; i < weights.length; i++) {
            float error = lastOutput[i] - target[i];
            float deriv = 1 - lastOutput[i]*lastOutput[i];
            delta[i] = error * deriv;
        }

        for (int r = 0; r < weights.length; r++) {
            for (int c = 0; c < weights[0].length; c++) {
                weights[r][c] -= lr * delta[r] * lastInput[c];
            }
            bias[r] -= lr * delta[r];
        }

        return delta;
    }
    public float[][] getWeights() {
        return weights.clone();
    }
    public String toString() {
        return weights[0].length+" "+weights.length+"\n";
    }
    public void write(PrintWriter out) {
        out.print(weights[0].length+" "+weights.length+"\n");
        for (int r=0; r<weights.length; r++) {
            out.print(Arrays.toString(weights[r]).replaceAll("[,\\[\\]]",""));
            out.print("\n");
        }
        out.print(Arrays.toString(bias).replaceAll("[,\\[\\]]",""));
        out.print("\n");
    }
}