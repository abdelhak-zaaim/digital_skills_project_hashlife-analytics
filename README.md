# HashLife Analytics: Cultural Hashtag Lifecycle Analyzer

> A Spring Boot web application for analyzing the evolution of cultural hashtags on X (formerly Twitter). Track the lifecycle, engagement metrics, and influence patterns of hashtags associated with cultural events in an interactive visual dashboard.

## Overview
HashLife Analytics is a web application designed to analyze the evolution and impact of cultural hashtags on X (formerly Twitter). The tool provides insights into how hashtags related to cultural events trend over time, showing patterns of engagement, related topics, and key influencers driving the conversation.

![HashLife Analytics Screenshot](https://via.placeholder.com/800x400.png?text=HashLife+Analytics+Screenshot)

## Features

### 🔍 Hashtag Trend Analysis
- Track tweet volume and engagement metrics over time
- Visualize the lifecycle of cultural hashtags (rise, peak, decline)
- Compare engagement patterns across different time periods

### 📊 Data Visualization
- Interactive graphs showing tweet volume and engagement metrics
- Daily breakdown of hashtag activity
- Visual representation of hashtag performance

### 👥 Influencer Identification
- Identify key accounts amplifying cultural hashtags
- Track the most active participants in hashtag conversations
- Discover content creators shaping the discourse

### 🔗 Related Hashtag Discovery
- Uncover connections between hashtags
- Identify emerging sub-topics within cultural conversations
- Map the ecosystem around major cultural events

### 📱 Sample Tweet Display
- View representative tweets using the analyzed hashtag
- See real engagement metrics for individual posts
- Understand context through content examples

## Technology Stack

- **Backend**: Java with Spring Boot
- **Frontend**: Thymeleaf, Bootstrap 5, Chart.js
- **API Integration**: Twitter API v2
- **Data Processing**: Java Stream API
- **Data Visualization**: Chart.js

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- Twitter Developer Account with API Bearer Token

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/hashlife-analytics.git
   cd hashlife-analytics
   ```

2. Configure your Twitter API credentials:
   - Open `src/main/resources/application.properties`
   - Add your Twitter API Bearer Token:
     ```properties
     twitter.api.bearer.token=YOUR_TWITTER_API_BEARER_TOKEN
     ```

3. Build the project:
   ```bash
   mvn clean install
   ```

4. Run the application:
   ```bash
   mvn spring-boot:run
   ```

5. Access the application in your browser:
   ```
   http://localhost:8080
   ```

## Usage Guide

### Analyzing a Hashtag

1. Navigate to the "Analyze Hashtag" page
2. Enter a cultural hashtag (e.g., "cannes2025", "metgala", "sundancefilm")
3. Select a date range (note: Twitter's standard API only allows searching the past 7 days)
4. Click "Analyze Hashtag" to generate insights
5. Review the visual analysis including:
   - Tweet volume trends
   - Engagement patterns
   - Related hashtags
   - Key influencers
   - Sample tweets

### Interpreting Results

- **Lifecycle Chart**: Shows how the conversation evolved over the selected time period
- **Engagement Metrics**: Indicates the level of interaction (likes, retweets, replies)
- **Related Hashtags**: Reveals other topics connected to the main conversation
- **Influencer List**: Highlights accounts driving the conversation
- **Sample Tweets**: Provides examples of typical content using the hashtag

## Limitations

- Twitter's standard API only allows searching tweets from the past 7 days
- Result limits are imposed by the Twitter API (max 100 tweets per request)
- Rate limiting may affect the completeness of data for very popular hashtags

## Future Enhancements

- Integration with historical Twitter data sources
- Advanced sentiment analysis of hashtag conversations
- Comparison of multiple hashtags side-by-side
- Export capabilities for reports and data
- Real-time monitoring of emerging cultural hashtags

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgements

- Twitter API for providing access to tweet data
- Spring Boot, Thymeleaf, and Bootstrap communities
- Chart.js for visualization capabilities
- All contributors to the project

---

*HashLife Analytics - Uncovering insights from hashtag trends and cultural movements on social media*
