package com.degital.skills.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tweet {
    private String id;
    private String text;
    private String username;
    private LocalDateTime createdAt;
    private int likeCount;
    private int retweetCount;
    private int replyCount;
    private int quoteCount;
    private List<String> hashtagsUsed;
}
