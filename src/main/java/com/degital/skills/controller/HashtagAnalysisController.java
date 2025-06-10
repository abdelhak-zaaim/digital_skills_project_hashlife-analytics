package com.degital.skills.controller;

import com.degital.skills.model.HashtagAnalysis;
import com.degital.skills.service.TwitterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
public class HashtagAnalysisController {

    @Autowired
    private TwitterService twitterService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/analysis")
    public String analysisForm() {
        return "analysis-form";
    }

    @PostMapping("/analyze")
    public String analyzeHashtag(
            @RequestParam String hashtag,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        // Remove # if present
        if (hashtag.startsWith("#")) {
            hashtag = hashtag.substring(1);
        }

        HashtagAnalysis analysis = twitterService.analyzeHashtag(hashtag, startDate, endDate);
        model.addAttribute("analysis", analysis);

        return "analysis-result";
    }
}
