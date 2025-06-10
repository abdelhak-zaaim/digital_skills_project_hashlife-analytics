package com.degital.skills.service;

import com.degital.skills.model.HashtagAnalysis;
import com.degital.skills.model.Tweet;
import io.github.redouane59.twitter.TwitterClient;
import io.github.redouane59.twitter.dto.tweet.TweetList;
import io.github.redouane59.twitter.dto.tweet.TweetV2;
import io.github.redouane59.twitter.signature.TwitterCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TwitterService {

    @Value("${twitter.api.bearer.token}")
    private String bearerToken;

    private TwitterClient getTwitterClient() {
        return new TwitterClient(TwitterCredentials.builder()
                .bearerToken(bearerToken)
                .build());
    }

    public HashtagAnalysis analyzeHashtag(String hashtag, LocalDate startDate, LocalDate endDate) {
        TwitterClient client = getTwitterClient();
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // Search for tweets with the hashtag
        TweetList tweetList;
        try {
            // Use the correct signature for searchTweets
            tweetList = client.searchTweets("#" + hashtag);

            // Filter tweets by date manually since we can't use date params directly
            if (tweetList != null && tweetList.getData() != null) {
                tweetList.getData().removeIf(tweet -> {
                    if (tweet.getCreatedAt() == null) return true;
                    LocalDateTime tweetDate = tweet.getCreatedAt()
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
                    return tweetDate.isBefore(startDateTime) || tweetDate.isAfter(endDateTime);
                });
            }
        } catch (Exception e) {
            // Log error and return an empty analysis if API call fails
            System.err.println("Error accessing Twitter API: " + e.getMessage());
            return createEmptyAnalysis(hashtag, startDateTime, endDateTime);
        }

        // Check if we have data
        if (tweetList == null || tweetList.getData() == null || tweetList.getData().isEmpty()) {
            return createEmptyAnalysis(hashtag, startDateTime, endDateTime);
        }

        List<Tweet> tweets = new ArrayList<>();
        Map<String, Integer> tweetsByDay = new HashMap<>();
        Map<String, Integer> engagementByDay = new HashMap<>();
        Map<String, Integer> relatedHashtagsCount = new HashMap<>();
        Map<String, Integer> influencerCount = new HashMap<>();

        // Process all found tweets
        for (TweetV2.TweetData tweetData : tweetList.getData()) {
            // Convert TweetData to your Tweet model
            Tweet tweet = convertToTweet(tweetData);
            tweets.add(tweet);

            // Count tweets by day
            String day = tweet.getCreatedAt().toLocalDate().toString();
            tweetsByDay.put(day, tweetsByDay.getOrDefault(day, 0) + 1);

            // Calculate engagement by day (likes + retweets + quotes + replies)
            int engagement = tweet.getLikeCount() + tweet.getRetweetCount() +
                            tweet.getQuoteCount() + tweet.getReplyCount();
            engagementByDay.put(day, engagementByDay.getOrDefault(day, 0) + engagement);

            // Count related hashtags
            if (tweet.getHashtagsUsed() != null) {
                for (String tag : tweet.getHashtagsUsed()) {
                    if (!tag.equalsIgnoreCase(hashtag)) {
                        relatedHashtagsCount.put(tag, relatedHashtagsCount.getOrDefault(tag, 0) + 1);
                    }
                }
            }

            // Count influencers
            influencerCount.put(tweet.getUsername(),
                             influencerCount.getOrDefault(tweet.getUsername(), 0) + 1);
        }

        // Get top related hashtags
        List<String> topRelatedHashtags = getTopItems(relatedHashtagsCount, 10);

        // Get top influencers
        List<String> topInfluencers = getTopItems(influencerCount, 10);

        // Create and return the hashtag analysis
        return new HashtagAnalysis(
            hashtag,
            startDateTime,
            endDateTime,
            tweets.size(),
            tweets.stream().mapToInt(Tweet::getRetweetCount).sum(),
            tweets.stream().mapToInt(Tweet::getLikeCount).sum(),
            tweets.stream().limit(5).collect(Collectors.toList()),
            tweetsByDay,
            engagementByDay,
            topRelatedHashtags,
            topInfluencers
        );
    }

    private Tweet convertToTweet(TweetV2.TweetData tweetData) {
        Tweet tweet = new Tweet();
        tweet.setId(tweetData.getId());
        tweet.setText(tweetData.getText());

        // Safely handle user info
        if (tweetData.getUser() != null) {
            tweet.setUsername(tweetData.getUser().getName());
        } else {
            tweet.setUsername("Unknown");
        }

        // Handle date conversion safely
        if (tweetData.getCreatedAt() != null) {
            // Convert directly to LocalDateTime using ZoneId
            tweet.setCreatedAt(
                LocalDateTime.ofInstant(
                    tweetData.getCreatedAt().toInstant(java.time.ZoneOffset.UTC),
                    ZoneId.systemDefault()
                )
            );
        } else {
            tweet.setCreatedAt(LocalDateTime.now());
        }

        // Set engagement metrics - handle primitive int fields (can't be null)
        tweet.setLikeCount(tweetData.getLikeCount());
        tweet.setRetweetCount(tweetData.getRetweetCount());
        tweet.setReplyCount(tweetData.getReplyCount());
        tweet.setQuoteCount(tweetData.getQuoteCount());

        // Extract hashtags
        List<String> hashtags = new ArrayList<>();
        if (tweetData.getEntities() != null && tweetData.getEntities().getHashtags() != null) {
            hashtags = tweetData.getEntities().getHashtags().stream()
                .map(hashtagEntity -> hashtagEntity.getText())
                .collect(Collectors.toList());
        }
        tweet.setHashtagsUsed(hashtags);

        return tweet;
    }

    private <T> List<String> getTopItems(Map<T, Integer> countMap, int limit) {
        return countMap.entrySet().stream()
            .sorted(Map.Entry.<T, Integer>comparingByValue().reversed())
            .limit(limit)
            .map(entry -> entry.getKey().toString())
            .collect(Collectors.toList());
    }

    private HashtagAnalysis createEmptyAnalysis(String hashtag, LocalDateTime start, LocalDateTime end) {
        return new HashtagAnalysis(
            hashtag,
            start,
            end,
            0,
            0,
            0,
            Collections.emptyList(),
            Collections.emptyMap(),
            Collections.emptyMap(),
            Collections.emptyList(),
            Collections.emptyList()
        );
    }
}
