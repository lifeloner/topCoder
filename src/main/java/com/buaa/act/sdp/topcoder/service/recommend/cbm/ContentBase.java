package com.buaa.act.sdp.topcoder.service.recommend.cbm;

import com.buaa.act.sdp.topcoder.util.Maths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by yang on 2017/2/23.
 */
@Component
public class ContentBase {

    private static final Logger logger = LoggerFactory.getLogger(ContentBase.class);

    /**
     * 获取相似任务中所有的获胜者handle
     *
     * @param winner    所有的获胜者
     * @param neighbors 相似的任务
     * @return
     */
    public Set<String> getWinner(List<String> winner, List<Integer> neighbors) {
        logger.info("get the winners in the similar tasks");
        Set<String> set = new HashSet<>();
        for (int i = 0; i < neighbors.size(); i++) {
            set.add(winner.get(neighbors.get(i)));
        }
        return set;
    }

    /**
     * 计算当前任务与其相似任务之间的相似度，并按照相似度排序
     *
     * @param features      特征向量
     * @param index         当前任务下标
     * @param neighborIndex 相似的任务
     * @return
     */
    public double[][] getSimilarityTasks(double[][] features, int index, List<Integer> neighborIndex) {
        logger.info("get the similar tasks' feature vector for a new task");
        double[][] similarity = new double[neighborIndex.size()][2];
        for (int i = 0; i < neighborIndex.size(); i++) {
            similarity[i][0] = neighborIndex.get(i);
            similarity[i][1] = Maths.taskSimilariry(features[index], features[neighborIndex.get(i)]);
        }
        Arrays.sort(similarity, new Comparator<double[]>() {
            @Override
            public int compare(double[] o1, double[] o2) {
                return Double.compare(o2[1], o1[1]);
            }
        });
        return similarity;
    }

    /**
     * 取前20个相似任务,item-based推荐
     *
     * @param features 特征向量
     * @param index    任务小标
     * @param scores   开发者得分
     * @param winner   开发者获胜者
     * @return
     */
    public Map<String, Double> getRecommendResult(double[][] features, int index, List<Map<String, Double>> scores, List<String> winner) {
        logger.info("get the recommended developers for new task using CF");
        Map<String, List<Double>> map = new HashMap<>();
        List<Integer> neighborIndex = Maths.getSimilarityTasks(features, index);
        double[][] similarity = getSimilarityTasks(features, index, neighborIndex);
        Set<String> winnerSet = getWinner(winner, neighborIndex);
        for (int i = 0; i < neighborIndex.size(); i++) {
            for (Map.Entry<String, Double> entry : scores.get((int) similarity[i][0]).entrySet()) {
                if (winnerSet.contains(entry.getKey())) {
                    if (map.containsKey(entry.getKey())) {
                        map.get(entry.getKey()).add(entry.getValue());
                        map.get(entry.getKey()).add(similarity[i][1]);
                    } else {
                        List<Double> list = new ArrayList<>();
                        list.add(entry.getValue());
                        list.add(similarity[i][1]);
                        map.put(entry.getKey(), list);
                    }
                }
            }
        }
        double score, weight;
        Map<String, Double> workerMap = new HashMap<>();
        for (Map.Entry<String, List<Double>> entry : map.entrySet()) {
            List<Double> list = entry.getValue();
            score = 0;
            weight = 0;
            for (int i = 0; i < list.size(); i += 2) {
                score += list.get(i) * list.get(i + 1);
                weight += list.get(i + 1);
            }
            score = score / weight;
            workerMap.put(entry.getKey(), score);
        }
        return workerMap;
    }

}
