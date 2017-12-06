package com.buaa.act.sdp.topcoder.service.statistics;

import com.buaa.act.sdp.topcoder.model.challenge.ChallengeItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by yang on 2017/5/31.
 */
@Component
public class MsgFilter {

    private static final Logger logger = LoggerFactory.getLogger(MsgFilter.class);

    @Autowired
    private ProjectMsg projectMsg;

    /**
     * 对challenge进行过滤
     *
     * @param challengeItem
     * @param challengeType
     * @return
     */
    public boolean filterChallenge(ChallengeItem challengeItem, String challengeType) {
        if (!challengeItem.getCurrentStatus().equals("Completed")) {
            return false;
        }
        String str = challengeItem.getChallengeType();
        if (!str.equals(challengeType)) {
            return false;
        }
        if (challengeItem.getDetailedRequirements() == null || challengeItem.getDetailedRequirements().length() == 0) {
            return false;
        }
        if (challengeItem.getTechnology() == null || challengeItem.getTechnology().length == 0 || challengeItem.getTechnology()[0].isEmpty()) {
            return false;
        }
        if (challengeItem.getChallengeName() == null || challengeItem.getChallengeName().length() == 0) {
            return false;
        }
        if (challengeItem.getDuration() == 0) {
            return false;
        }
        if (challengeItem.getPrize() == null || challengeItem.getPrize().length == 0 || challengeItem.getPrize()[0].isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * 获取一个project之前的project任务，只需要三种类型任务
     *
     * @param projectId
     * @return
     */
    public List<List<Integer>> getProjectAndChallenges(int projectId) {
        logger.info("get all tasks id in projects before new project,projectId=" + projectId);
        List<List<Integer>> list = new ArrayList<>();
        Map<Integer, List<Integer>> projectIdToChallengeIds = projectMsg.getProjectToChallenges();
        for (Map.Entry<Integer, List<Integer>> entry : projectIdToChallengeIds.entrySet()) {
            if (entry.getKey() < projectId) {
                list.add(entry.getValue());
            }
        }
        return list;
    }
}