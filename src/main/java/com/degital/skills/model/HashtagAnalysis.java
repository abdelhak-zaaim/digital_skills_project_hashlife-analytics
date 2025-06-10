package com.degital.skills.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HashtagAnalysis {
    private String hashtag;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private int totalTweets;
    private int totalRetweets;
    private int totalLikes;
    private List<Tweet> sampleTweets;
    private Map<String, Integer> tweetsByDay;
    private Map<String, Integer> engagementByDay;
    private List<String> relatedHashtags;
    private List<String> topInfluencers;
}
