data <- read.csv("armse.csv")
data <- data[2:11]

rankMatrix <- function(data, ...){

  #f <- rank(data, ties.method="average")
  
  rankings <- t(apply (data, MARGIN=1, FUN=rank))
  colnames(rankings) <- colnames(data)
  rownames(rankings) <- rownames(data)
  return(rankings)
}

imanDavenportTest <- function (data, ...) {
  N <- dim(data)[1]
  k <- dim(data)[2]
  mr <- colMeans(rankMatrix(data))
  
  friedman.stat <- 12 * N / (k * (k + 1)) * (sum(mr^2) - (k * (k + 1)^2) / 4)
  # Iman Davenport correction of Friedman's test
  id.stat <- (N - 1) * friedman.stat / (N * (k - 1) - friedman.stat)
  p.value <- 1 - pf(id.stat, df1=(k - 1), df2=(k - 1) * (N - 1))
  
  names(id.stat)<-"Corrected Friedman's chi-squared"
  parameter <- c((k - 1), (k - 1) * (N - 1))
  names(parameter) <- c("df1", "df2")
  method <- "Iman Davenport's correction of Friedman's rank sum test"
  data.name <- deparse(substitute(data))
  htest.result <- list(statistic=id.stat, parameter=parameter, 
                       p.value=p.value, method=method, data.name=data.name)
  class(htest.result)<-"htest"
  htest.result
}

result <- imanDavenportTest(data)

cat result

# got the F value from distribution for FWE of 0.05 with 8df1 and 144df2
cat 1.67020*sqrt(90/(6*19))
