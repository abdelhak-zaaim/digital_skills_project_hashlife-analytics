package com.degital.skills.service;

import com.degital.skills.model.HashtagAnalysis;
import com.degital.skills.model.Tweet;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TwitterService {

    private static final int MAX_RESULTS = 100;

    @Value("${twitter.api.bearer.token}")
    private String bearerToken;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String TWITTER_API_BASE_URL = "https://api.twitter.com/2/tweets/search/recent";

    public HashtagAnalysis analyzeHashtag(String hashtag, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // Build the query URL with the hashtag
        String query = "#" + hashtag + " -is:retweet lang:en";

        // Format dates for Twitter API
        String startTime = startDateTime.format(DateTimeFormatter.ISO_DATE_TIME) + "Z";
        String endTime = endDateTime.format(DateTimeFormatter.ISO_DATE_TIME) + "Z";

        // Build the complete URL with parameters
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(TWITTER_API_BASE_URL)
            .queryParam("query", query)
            .queryParam("max_results", MAX_RESULTS)
            .queryParam("tweet.fields", "created_at,public_metrics,entities")
            .queryParam("expansions", "author_id")
            .queryParam("user.fields", "name,username")
            .queryParam("start_time", startTime)
            .queryParam("end_time", endTime);

        String twitterApiUrl = builder.build().encode().toUriString();

        List<Tweet> tweets = new ArrayList<>();
        Map<String, Integer> tweetsByDay = new HashMap<>();
        Map<String, Integer> engagementByDay = new HashMap<>();
        Map<String, Integer> relatedHashtagsCount = new HashMap<>();
        Map<String, Integer> influencerCount = new HashMap<>();

        try {
            // Set up HTTP headers with the bearer token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + bearerToken);

            // Make the API request
            ResponseEntity<String> response = restTemplate.exchange(
                twitterApiUrl,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class
            );

            // Parse the JSON response
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode tweetData = root.get("data");
            JsonNode includes = root.get("includes");

            // Create a map of user IDs to usernames for easy lookup
            Map<String, String> userMap = new HashMap<>();
            if (includes != null && includes.has("users")) {
                JsonNode users = includes.get("users");
                for (JsonNode user : users) {
                    userMap.put(user.get("id").asText(), user.get("name").asText());
                }
            }

            // Process all tweets
            if (tweetData != null && tweetData.isArray()) {
                for (JsonNode tweetNode : tweetData) {
                    Tweet tweet = convertJsonToTweet(tweetNode, userMap);
                    tweets.add(tweet);

                    // Count tweets by day
                    String day = tweet.getCreatedAt().toLocalDate().toString();
                    tweetsByDay.put(day, tweetsByDay.getOrDefault(day, 0) + 1);

                    // Calculate engagement by day
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
                    influencerCount.put(tweet.getUsername(), influencerCount.getOrDefault(tweet.getUsername(), 0) + 1);
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching data from Twitter API: " + e.getMessage());
            e.printStackTrace();
            return createEmptyAnalysis(hashtag, startDateTime, endDateTime);
        }

        // If no tweets were found, return empty analysis
        if (tweets.isEmpty()) {
            return createEmptyAnalysis(hashtag, startDateTime, endDateTime);
        }

        // Get top related hashtags and influencers
        List<String> topRelatedHashtags = getTopItems(relatedHashtagsCount, 10);
        List<String> topInfluencers = getTopItems(influencerCount, 10);

        // Create and return hashtag analysis
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

    private Tweet convertJsonToTweet(JsonNode tweetNode, Map<String, String> userMap) {
        Tweet tweet = new Tweet();

        // Extract basic tweet info
        tweet.setId(tweetNode.get("id").asText());
        tweet.setText(tweetNode.get("text").asText());

        // Set username from user map
        String authorId = tweetNode.has("author_id") ? tweetNode.get("author_id").asText() : null;
        tweet.setUsername(authorId != null && userMap.containsKey(authorId) ?
                         userMap.get(authorId) : "Unknown");

        // Handle created_at date
        if (tweetNode.has("created_at")) {
            String createdAt = tweetNode.get("created_at").asText();
            tweet.setCreatedAt(LocalDateTime.ofInstant(
                Instant.parse(createdAt),
                ZoneId.systemDefault()
            ));
        } else {
            tweet.setCreatedAt(LocalDateTime.now());
        }

        // Handle public metrics
        if (tweetNode.has("public_metrics")) {
            JsonNode metrics = tweetNode.get("public_metrics");
            tweet.setLikeCount(metrics.has("like_count") ? metrics.get("like_count").asInt() : 0);
            tweet.setRetweetCount(metrics.has("retweet_count") ? metrics.get("retweet_count").asInt() : 0);
            tweet.setReplyCount(metrics.has("reply_count") ? metrics.get("reply_count").asInt() : 0);
            tweet.setQuoteCount(metrics.has("quote_count") ? metrics.get("quote_count").asInt() : 0);
        } else {
            tweet.setLikeCount(0);
            tweet.setRetweetCount(0);
            tweet.setReplyCount(0);
            tweet.setQuoteCount(0);
        }

        // Extract hashtags
        List<String> hashtags = new ArrayList<>();
        if (tweetNode.has("entities") && tweetNode.get("entities").has("hashtags")) {
            JsonNode hashtagNodes = tweetNode.get("entities").get("hashtags");
            for (JsonNode hashtagNode : hashtagNodes) {
                if (hashtagNode.has("tag")) {
                    hashtags.add(hashtagNode.get("tag").asText());
                }
            }
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
