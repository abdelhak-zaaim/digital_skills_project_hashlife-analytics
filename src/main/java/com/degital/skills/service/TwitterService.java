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
        TweetList tweetList = client.searchTweets("#" + hashtag, null,
                                               startDateTime, endDateTime, 100);

        List<Tweet> tweets = new ArrayList<>();
        Map<String, Integer> tweetsByDay = new HashMap<>();
        Map<String, Integer> engagementByDay = new HashMap<>();
        Map<String, Integer> relatedHashtagsCount = new HashMap<>();
        Map<String, Integer> influencerCount = new HashMap<>();

        // Process all found tweets
        for (TweetV2 tweetV2 : tweetList.getData()) {
            Tweet tweet = convertToTweet(tweetV2);
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

    private Tweet convertToTweet(TweetV2 tweetV2) {
        Tweet tweet = new Tweet();
        tweet.setId(tweetV2.getId());
        tweet.setText(tweetV2.getText());
        tweet.setUsername(tweetV2.getUser().getName());
        tweet.setCreatedAt(
            tweetV2.getCreatedAt()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
        );
        tweet.setLikeCount(tweetV2.getLikeCount());
        tweet.setRetweetCount(tweetV2.getRetweetCount());
        tweet.setReplyCount(tweetV2.getReplyCount());
        tweet.setQuoteCount(tweetV2.getQuoteCount());

        // Extract hashtags
        List<String> hashtags = new ArrayList<>();
        if (tweetV2.getEntities() != null && tweetV2.getEntities().getHashtags() != null) {
            hashtags = tweetV2.getEntities().getHashtags().stream()
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
}
