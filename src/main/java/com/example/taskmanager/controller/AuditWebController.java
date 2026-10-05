package com.example.taskmanager.controller;

import com.example.taskmanager.repository.AuditLogRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/web/audit")
public class AuditWebController {

    private final AuditLogRepository auditLogRepository;

    public AuditWebController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("logs",
                auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "id")));
        return "audit/list";
    }
}