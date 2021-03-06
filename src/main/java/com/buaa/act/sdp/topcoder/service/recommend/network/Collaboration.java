package com.buaa.act.sdp.topcoder.service.recommend.network;

import com.buaa.act.sdp.topcoder.service.statistics.TaskScores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by yang on 2017/3/17.
 */
@Component
public class Collaboration {

    private static final Logger logger = LoggerFactory.getLogger(Collaboration.class);

    @Autowired
    private TaskScores taskScores;

    /**
     * 一个project内的协作统计
     *
     * @param workerIndex 开发者下标
     * @param colCount    开发者之间协作次数
     * @param taskCount   完成任务数量
     * @param colScores   协作开发者得分
     * @param score       一个项目内每个任务的开发者得分信息
     */
    public void generateCollaborationInProject(Map<String, Integer> workerIndex, int[][] colCount, int[] taskCount, double[][] colScores, List<Map<String, Double>> score) {
        Set<String> set = new HashSet<>();
        Map<String, Double> map;
        int m, n;
        for (int i = 0; i < score.size(); i++) {
            map = score.get(i);
            List<String> list = new ArrayList<>();
            if (map != null && !map.isEmpty()) {
                for (Map.Entry<String, Double> entry : map.entrySet()) {
                    if (workerIndex.containsKey(entry.getKey())) {
                        list.add(entry.getKey());
                        m = workerIndex.get(entry.getKey());
                        taskCount[m]++;
                        for (String user : set) {
                            n = workerIndex.get(user);
                            colCount[m][n]++;
                            colCount[n][m]++;
                            colScores[m][n] += entry.getValue();
                            colScores[n][m] += entry.getValue();
                        }
                    }
                }
                set.addAll(list);
            }
        }
    }

    /**
     * 根据 统计的任务数量及得分情况，计算developer之间协作强度
     *
     * @param colCount
     * @param taskCount
     * @param colScores
     * @return
     */
    public double[][] computeCollaboration(int[][] colCount, int[] taskCount, double[][] colScores) {
        logger.info("compute the collaboration among developers in a project");
        double[][] result = new double[colCount.length][colCount.length];
        int sum;
        for (int i = 0; i < colCount.length; i++) {
            for (int j = i; j < colCount.length; j++) {
                sum = taskCount[i] + taskCount[j];
                if (sum != 0) {
                    result[i][j] = 1.0 * colCount[i][j] / sum + colScores[i][j] / sum / 100;
                    result[j][i] = 1.0 * colCount[i][j] / sum + colScores[i][j] / sum / 100;
                } else {
                    result[i][j] = 0;
                    result[j][i] = 0;
                }
            }
        }
        return result;
    }

    /**
     * 根据历史任务，计算开发者之间的协作强度
     *
     * @param workerIndex
     * @param taskIds
     * @return
     */
    public double[][] generateCollaboration(Map<String, Integer> workerIndex, List<List<Integer>> taskIds) {
        logger.info("generate the collaboration between developers in a project");
        Map<Integer, Map<String, Double>> scores = taskScores.getDevelopersScores();
        int[][] colCount = new int[workerIndex.size()][workerIndex.size()];
        int[] taskCount = new int[workerIndex.size()];
        double[][] colScores = new double[workerIndex.size()][workerIndex.size()];
        logger.info("construct the competitive network among developers in a project");
        for (List<Integer> list : taskIds) {
            List<Map<String, Double>> score = new ArrayList<>(list.size());
            for (int taskId : list) {
                if (scores.containsKey(taskId)) {
                    score.add(scores.get(taskId));
                }
            }
            generateCollaborationInProject(workerIndex, colCount, taskCount, colScores, score);
        }
        return computeCollaboration(colCount, taskCount, colScores);
    }
}
