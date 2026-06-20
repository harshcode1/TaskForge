package com.projectmgmttool.backend.service;

import com.projectmgmttool.backend.entity.Task;
import com.projectmgmttool.backend.entity.enums.TaskStatus;
import com.projectmgmttool.backend.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReminderService {

    private static final Logger logger = LoggerFactory.getLogger(ReminderService.class);

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private MailSender mailSender;

    @Scheduled(cron = "0 0 8 * * ?") // Runs daily at 8 AM
    public void sendTaskReminders() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        // Fetch tasks due today or tomorrow with PENDING or IN_PROGRESS status
        List<Task> tasks = taskRepository.findByDueDateBetweenAndStatusIn(
                today, tomorrow, List.of(TaskStatus.PENDING, TaskStatus.IN_PROGRESS));

        for (Task task : tasks) {
            sendEmailReminder(task);
        }
    }

    private void sendEmailReminder(Task task) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(task.getAssignee().getEmail());
            message.setSubject("Task Reminder: " + task.getTitle());
            message.setText("Dear " + task.getAssignee().getName() + ",\n\n" +
                    "This is a reminder for your task: \"" + task.getTitle() + "\".\n" +
                    "Due Date: " + task.getDueDate() + "\n\n" +
                    "Please ensure it is completed on time.\n\n" +
                    "Best regards,\nTaskForge Team");

            mailSender.send(message);
        } catch (Exception e) {
            // Log error (logging framework can be added)
            logger.error("Failed to send email reminder for task: " + task.getId(), e);
        }
    }
}
