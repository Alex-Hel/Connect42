import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

public class Network {
    private Layer[] layers;
    private float alpha;

    public Network(float learning_rate, int... sizes) {
        layers = new Layer[sizes.length-1];
        for (int i=0; i<sizes.length-1; i++) {
            layers[i] = new Layer(sizes[i],sizes[i+1]);
        }
        alpha = learning_rate;
    }
    public void train(float[] input, float[] target) {
        float[] prev = input;
        for (Layer L : layers) {
            prev = L.forwardPass(prev);
        }
        float[] delta = layers[layers.length-1].backwardOutput(target, alpha);
        for (int i = layers.length-2; i >= 0; i--) {
            delta = layers[i].backwardPropagate(delta, layers[i+1].getWeights(), alpha);
        }
    }
    public float[] predict(float[] input) {
        float[] prev = input;
        for (Layer L : layers) {
            prev = L.forwardPass(prev);
        }
        return prev;
    }
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Neural_Network!_with_this_many_layers-> "+layers.length+"\n");
        sb.append(alpha+"\n");
        for (Layer l : layers) {
            sb.append(l.toString());
        }
        return sb.toString();
    }
    public boolean write(String fileName) {
        try (PrintWriter out = new PrintWriter(new FileWriter(fileName))) {
            out.print(this.toString());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    public Network(String fileName) {
        try {
            Scanner s = new Scanner(new File(fileName));
            s.next(); //label
            layers = new Layer[s.nextInt()];
            alpha = s.nextFloat();
            for (int i=0; i<layers.length; i++) {
                int input = s.nextInt();
                int output = s.nextInt();
                float[][] weights = new float[output][input];
                for (int r=0; r<output; r++) {
                    for (int c=0; c<input; c++) {
                        weights[r][c] = s.nextFloat();
                    }
                }
                float[] bias = new float[output];
                for (int b=0; b<output; b++) {
                    bias[b] = s.nextFloat();
                }
                layers[i] = new Layer(weights,bias);
            }
        } catch (IOException e) {
            e.printStackTrace();
            for (Layer l : layers) {
                l.randomize();
            }
            alpha = .05f;
        }
    }
}
