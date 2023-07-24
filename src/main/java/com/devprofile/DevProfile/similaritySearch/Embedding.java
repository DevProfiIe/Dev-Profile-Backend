package com.devprofile.DevProfile.similaritySearch;

import com.github.jfasttext.FastTextWrapper;
import com.github.jfasttext.JFastText;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class Embedding {

    JFastText jFastText = new JFastText();

    public void loadModel(){
        jFastText.loadModel("C:/DevRoot/Model/cc.en.300.bin");
    }

    public double[] getVector(String query){
        List<Float> floatListVec=jFastText.getVector(query);
        return floatListVec.stream()
                .mapToDouble(Float::doubleValue)
                .toArray();
    }

    public double cosineSimilarity(double[] vectorA, double[] vectorB) {
        RealVector vA = new ArrayRealVector(vectorA);
        RealVector vB = new ArrayRealVector(vectorB);

        double dotProduct = vA.dotProduct(vB);
        double normA = vA.getNorm();
        double normB = vB.getNorm();

        return dotProduct / (normA * normB);
    }
}
