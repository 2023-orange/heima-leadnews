package com.heima.schedule.service.Impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.common.constants.ScheduleConstants;
import com.heima.common.redis.CacheService;
import com.heima.model.schedule.dtos.Task;
import com.heima.model.schedule.pojos.Taskinfo;
import com.heima.model.schedule.pojos.TaskinfoLogs;
import com.heima.schedule.mapper.TaskinfoLogsMapper;
import com.heima.schedule.mapper.TaskinfoMapper;
import com.heima.schedule.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@Transactional
public class TaskServiceImpl implements TaskService {
    @Override
    public long addTask(Task task) {
        //添加任务到数据库中
        boolean succes = addTaskToDb(task);
        if (succes){
            //添加任务到redis
            addTaskToCache(task);
        }
        return task.getTaskId();
    }

    @Autowired
    private CacheService cacheService;
    /**
     * 把任务添加到Redis中
     * @param task
     */
    private void addTaskToCache(Task task) {
        String key = task.getTaskType() + "_" + task.getPriority();
        //获取5分钟之后的时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE,5);
        long nextScheduleTime = calendar.getTimeInMillis();
        //如果任务的执行时间小于等于当前时间，存入List
        if (task.getExecuteTime() <= System.currentTimeMillis()){
            cacheService.lLeftPush(ScheduleConstants.TOPIC + key, JSON.toJSONString(task));
        }else if (task.getExecuteTime() <= nextScheduleTime){
            //如果任务的执行时间大于等于当前时间并且小于等于预设时间（未来5分钟），存入Zset
            cacheService.zAdd(ScheduleConstants.FUTURE + key, JSON.toJSONString(task),task.getExecuteTime());
        }

    }

    @Autowired
    private TaskinfoMapper taskinfoMapper;

    @Autowired
    private TaskinfoLogsMapper taskinfoLogsMapper;
    /**
     *添加任务到数据库中
     * @param task
     * @return
     */
    private boolean addTaskToDb(Task task) {

        boolean flag = false;
        try {
            //保存任务表
            Taskinfo taskinfo = new Taskinfo();
            BeanUtils.copyProperties(task,taskinfo);
            taskinfo.setExecuteTime(new Date(task.getExecuteTime()));
            taskinfoMapper.insert(taskinfo);

            //设置taskId
            task.setTaskId(taskinfo.getTaskId());
            //保存任务日志数据
            TaskinfoLogs taskinfoLogs = new TaskinfoLogs();
            BeanUtils.copyProperties(taskinfo, taskinfoLogs);
            taskinfoLogs.setVersion(1);
            taskinfoLogs.setStatus(ScheduleConstants.SCHEDULED);
            taskinfoLogsMapper.insert(taskinfoLogs);

            flag = true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return flag;
    }

    /**
     * 取消任务
     * @param taskId
     * @return
     */
    @Override
    public boolean cancelTask(long taskId) {
        boolean flag = false;
        //删除任务，更新任务日志
        Task task = updateDb(taskId,ScheduleConstants.CANCELLED);
        //删除redis的数据
        if (task != null){
            removeTaskFromCache(task);
            flag = true;
        }
        return flag;
    }

    /**
     * 删除Redis中的数据
     * @param task
     */
    private void removeTaskFromCache(Task task) {
        String key = task.getTaskType() + "_" + task.getPriority();
        if (task.getExecuteTime()<=System.currentTimeMillis()){
            cacheService.lRemove(ScheduleConstants.TOPIC+key, 0, JSON.toJSONString(task));
        }else {
            cacheService.zRemove(ScheduleConstants.FUTURE+key, JSON.toJSONString(task));
        }
    }

    /**
     * 删除任务，更新任务日志
     * @param taskId
     * @param status
     * @return
     */
    private Task updateDb(long taskId, int status) {

        Task task = null;

        try {
            //删除任务
            taskinfoMapper.deleteById(taskId);
            //更新任务日志
            TaskinfoLogs taskinfoLogs = taskinfoLogsMapper.selectById(taskId);
            taskinfoLogs.setStatus(status);
            taskinfoLogsMapper.updateById(taskinfoLogs);
            task = new Task();
            BeanUtils.copyProperties(taskinfoLogs, task );
            task.setExecuteTime(taskinfoLogs.getExecuteTime().getTime());

        }catch (Exception e){
            log.error("task cancel exception taskId={}",taskId);
        }
        return task;
    }

    /**
     * 按照任务的类型和优先级拉取任务
     *
     * @param type
     * @param priority
     * @return
     */
    @Override
    public Task poll(int type, int priority) {
        Task task = null;
        try {
            String key = type + "_" +priority;
            //从Redis中拉取数据
            String task_json = cacheService.lLeftPop(ScheduleConstants.TOPIC + key);
            if (StringUtils.isNotBlank(task_json)){
                task = JSON.parseObject(task_json, Task.class);

                //修改数据库信息
                updateDb(task.getTaskId(), ScheduleConstants.EXECUTED);
            }
        }catch (Exception e){
            e.printStackTrace();
            log.error("poll task exception");
        }
        return task;
    }

    /**
     * 未来数据定时刷新
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void refresh(){

        String token = cacheService.tryLock("FUTURE_TASK_SYNC", 1000 * 30);

        if (StringUtils.isNotBlank(token)){
            log.info("未来数据定时刷新--定时任务");
            //获取所有未来数据的集合key
            Set<String> futrueKeys = cacheService.scan(ScheduleConstants.FUTURE + "*");
            for (String futrueKey : futrueKeys) {
                //获取当前数据的key  topic_key
                String topicKey = ScheduleConstants.TOPIC + futrueKey.split(ScheduleConstants.FUTURE)[1];
                //按照key和分值查询符合的数据
                Set<String> tasks = cacheService.zRangeByScore(futrueKey, 0, System.currentTimeMillis());

                //同步数据
                if (!tasks.isEmpty()){
                    cacheService.refreshWithPipeline(futrueKey,topicKey,tasks);
                    log.info("成功将"+futrueKey+"刷新到了"+topicKey);
                }
        }
        }
    }


    /**
     * 数据库任务定时同步到Redis
     */
    @PostConstruct
    @Scheduled(cron = "0 */5 * * * ?")
    public void reloadData(){
        //清理缓存中的数据  list zset
        clearCache();
        //查询符合条件的任务   小于未来5分钟的数据
        //获取5分钟之后的时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE,5);
        List<Taskinfo> taskinfoList = taskinfoMapper.selectList(Wrappers.<Taskinfo>lambdaQuery().lt(Taskinfo::getExecuteTime, calendar.getTime()));

        //把任务添加到Redis
        if (taskinfoList!=null && taskinfoList.size()>0){
            for (Taskinfo taskinfo : taskinfoList) {
                Task task = new Task();
                BeanUtils.copyProperties(taskinfo, task);
                task.setExecuteTime(taskinfo.getExecuteTime().getTime());
                addTaskToCache(task);
            }
        }
        log.info("数据库的任务同步到了Redis");
    }

    /**
     * 清理缓存中的数据
     */
    public void clearCache(){
        Set<String> topicKeys = cacheService.scan(ScheduleConstants.TOPIC + "*");
        Set<String> futureKeys = cacheService.scan(ScheduleConstants.FUTURE + "*");
        cacheService.delete(topicKeys);
        cacheService.delete(futureKeys);
    }
}
