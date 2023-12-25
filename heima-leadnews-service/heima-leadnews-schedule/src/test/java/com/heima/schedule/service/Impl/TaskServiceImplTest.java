package com.heima.schedule.service.Impl;

import com.heima.model.schedule.dtos.Task;
import com.heima.schedule.ScheduleApplication;
import com.heima.schedule.service.TaskService;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = ScheduleApplication.class)
@RunWith(SpringRunner.class)
class TaskServiceImplTest {

    @Autowired
    private TaskService taskService;
    @Test
    void addTask() {
        for (int i = 0;i < 5;i++){
            Task task = new Task();
            task.setTaskType(100+i);
            task.setPriority(50);
            task.setParameters("task_test".getBytes());
            task.setExecuteTime(new Date().getTime() + 500 * i);
            long taskId = taskService.addTask(task);
        }
    }

    @Test
    void cancelTask(){
        taskService.cancelTask(1715272803420045313L);
    }
    @Test
    void testPoll(){
        Task task = taskService.poll(100, 50);
        System.out.println(task);
    }


}