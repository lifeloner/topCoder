package com.buaa.act.sdp.topcoder.service.basic;

import com.buaa.act.sdp.topcoder.common.Constant;
import com.buaa.act.sdp.topcoder.common.TCData;
import com.buaa.act.sdp.topcoder.dao.TaskItemDao;
import com.buaa.act.sdp.topcoder.model.developer.Registrant;
import com.buaa.act.sdp.topcoder.model.task.TaskItem;
import com.buaa.act.sdp.topcoder.service.api.statistics.TaskStatistics;
import com.buaa.act.sdp.topcoder.service.recommend.feature.FeatureExtract;
import com.buaa.act.sdp.topcoder.service.statistics.ProjectMsg;
import com.buaa.act.sdp.topcoder.service.statistics.TaskMsg;
import com.buaa.act.sdp.topcoder.service.statistics.TaskScores;
import com.buaa.act.sdp.topcoder.util.Maths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Created by yang on 2017/11/17.
 */
@Service
public class TaskService {

    private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

    @Autowired
    private TaskItemDao taskItemDao;
    @Autowired
    private TaskScores taskScores;
    @Autowired
    private TaskMsg taskMsg;
    @Autowired
    private FeatureExtract featureExtract;
    @Autowired
    private ProjectMsg projectMsg;
    @Autowired
    private TaskStatistics taskStatistics;

    public TaskItem getTaskById(int taskId) {
        logger.info("get task's detail msg from db,taskId=" + taskId);
        return taskItemDao.getTaskItemById(taskId);
    }

    public boolean projectExist(int projectId) {
        if (projectId <= 0) {
            return false;
        }
        int num = taskItemDao.projectExist(projectId);
        return num > 0 ? true : false;
    }

    public List<TaskItem> getProjectTasks(int projectId) {
        logger.info("query tasks in a project in db,projectId=" + projectId);
        if (projectId <= 0) {
            return null;
        }
        return taskItemDao.getProjectTasks(projectId, Constant.TASK_TYPE);
    }

    public TCData<TaskItem> getAllTasks(int pageNum, int pageSize) {
        logger.info("get all tasks' id from db");
        int offSet = (pageNum - 1) * pageSize;
        TCData<TaskItem> data = new TCData<>();
        List<TaskItem> items = taskItemDao.getTasks(offSet, pageSize, Constant.TASK_TYPE);
        data.setData(items);
        if (pageNum == 1) {
            data.setTotal(taskItemDao.getTasksTotalNum(Constant.TASK_TYPE));
        }
        return data;
    }

    public List<Registrant> getTaskRegistrants(int taskId) {
        logger.info("get task's registrants from db,taskId=" + taskId);
        Map<String, Double> score = taskScores.getTaskScore(taskId);
        if (score == null || score.size() == 0) {
            return null;
        }
        Map<String, String> registerTime = taskScores.getRegisterDate(taskId);
        Map<String, String> submitTime = taskScores.getSubmitDate(taskId);
        String winner = taskScores.getWinner(taskId);
        List<Registrant> registrants = new ArrayList<>(score.size());
        for (Map.Entry<String, Double> entry : score.entrySet()) {
            Registrant registrant = new Registrant(entry.getKey(), registerTime.get(entry.getKey()), submitTime.get(entry.getKey()), entry.getValue(), entry.getKey().equals(winner));
            registrants.add(registrant);
        }
        return registrants;
    }

    public int getMaxTaskId() {
        return Math.max(taskItemDao.getMaxTaskId() + 1, Constant.MAX_TASK_ID);
    }

    public int getMaxProjectId() {
        return taskItemDao.getMaxProjectId() + 1;
    }

    public void uploadTask(TaskItem item) {
        item.setChallengeId(getMaxTaskId());
        taskStatistics.updateTask(item, 0, 0);
        taskItemDao.insert(item);
    }

    /**
     * 获取当前任务的相似任务
     *
     * @param item
     * @return
     */
    public List<TaskItem> getSimilerTask(TaskItem item) {
        logger.info("get task's similar tasks from db,type=" + item.getChallengeType() + ",taskId=" + item.getChallengeId());
        List<TaskItem> items = taskMsg.getItems(item.getChallengeType());
        List<TaskItem> result = new ArrayList<>(10);
        Map<Integer, Double> similarity = new HashMap<>();
        Map<Integer, TaskItem> taskMap = new HashMap<>();
        Set<String> skills = featureExtract.getSkills();
        double[] feature = featureExtract.generateVector(skills, item);
        double[] temp;
        double similar;
        for (TaskItem taskItem : items) {
            if (taskItem.getChallengeId() != item.getChallengeId()) {
                temp = featureExtract.generateVector(skills, taskItem);
                if ((similar = Maths.taskSimilariry(feature, temp)) >= 0.8) {
                    similarity.put(taskItem.getChallengeId(), similar);
                    taskMap.put(taskItem.getChallengeId(), taskItem);
                }
            }
        }
        List<Map.Entry<Integer, Double>> list = new ArrayList<>(similarity.size());
        list.addAll(similarity.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
            @Override
            public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });
        int count = 10;
        for (int i = 0; i < list.size() && i < count; i++) {
            result.add(taskMap.get(list.get(i).getKey()));
        }
        return result;
    }

    public TCData<Integer> getAllProjectIds(int pageNum, int pageSize) {
        Set<Integer> projectIds = projectMsg.getProjectToTasksMapping().keySet();
        int total = projectIds.size(), begin = (pageNum - 1) * pageSize;
        List<Integer> result = new ArrayList<>(pageSize > total - begin ? total - begin : pageSize);
        int index = 0;
        for (int projectId : projectIds) {
            index++;
            if (index < begin) {
                continue;
            } else if (index >= begin && index < begin + pageSize) {
                result.add(projectId);
            } else {
                break;
            }
        }
        TCData<Integer> data = new TCData<>();
        if (pageNum == 1) {
            data.setTotal(total);
        }
        data.setData(result);
        return data;
    }

    public String[] getTaskTypes() {
        Set<String> taskTypes = Constant.TASK_TYPE;
        String[] types = new String[taskTypes.size()];
        int index = 0;
        for (String type : taskTypes) {
            types[index++] = type;
        }
        return types;
    }

    public String[] getTaskTechnologies() {
        return Constant.TECHNOLOGIES;
    }

    public String[] getTaskPlatforms() {
        return Constant.PLATFORMS;
    }

    public String[] getTaskLanguages() {
        return Constant.LANGUAGES;
    }
}
