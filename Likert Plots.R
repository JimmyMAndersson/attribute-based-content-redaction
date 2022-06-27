library(likert)

setwd(dirname(rstudioapi::getActiveDocumentContext()$path))
results <- read.csv('qualitative_results.csv')

range <- 1:6
visuals <- data.frame(range, range, range, range)
colnames(visuals) <- c('Question 1', 'Question 2', 'Question 3a', 'Question 3b')

factors <- 1:5
labels <- c('Very little effort', 'Little effort', 'Neutral effort', 'Much effort', 'Very much effort')
visuals['Question 1'] <- factor(results$Q1Effort, factors, labels = labels, ordered = T)
visuals['Question 2'] <- factor(results$Q2Effort, factors, labels = labels, ordered = T)
visuals['Question 3a'] <- factor(results$Q3_1Effort, factors, labels = labels, ordered = T)
visuals['Question 3b'] <- factor(results$Q3_2Effort, factors, labels = labels, ordered = T)

plot <- likert(visuals)
p <- plot(
  plot, 
  colors = c('#2393D2', '#4D84D0', '#6F73C5', '#8B60B3', '#A04A99'),
  text.size = 5
)

print(
  p 
  + labs(
    title = 'Ease of Use',
    subtitle = 'Experienced effort needed to derive answers to survey questions'
  ) 
  + theme(
      plot.title = element_text(hjust = 0.5, size = 26, face = 'bold'),
      text = element_text(hjust = 0.5, size = 16),
      plot.subtitle = element_text(hjust = 0.5, size = 16),
      axis.text = element_text(hjust = 0.5, size = 16),
      strip.text = element_text(hjust = 0.5, size = 16),
    )
)

