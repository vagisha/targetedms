#
# Copyright (c) 2011-2014 LabKey Corporation
#
# Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
#
# R script to create a quality control trending plot for TargetedMS assays, showing data plotted
# with the average and +/- 3 standard deviation range for the average of the data.
#
# Author: Josh Eckels, LabKey

# load R libraries
library(Rlabkey, quietly=TRUE);
library(Cairo, quietly=TRUE);
library(plotrix, quietly=TRUE);

# option for Titration vs. SinglePointControl
isTitration = labkey.url.params$isTitration

# create a list of the columns that are needed for the trending plot
colSelect = paste("PeptideId/Sequence", "SampleFileId/AcquiredTime", "RetentionTime", sep=",");

colFilter = rbind();

# either filter on start and end date or on max number of rows
maxRows = NA;
if (!is.null(labkey.url.params$MaxRows)) {
	maxRows = labkey.url.params$MaxRows;
} else {
    if (!is.null(labkey.url.params$StartDate)) {
	    colFilter=rbind(colFilter,makeFilter(c("SampleFileId/AcquiredTime","DATE_GREATER_THAN_OR_EQUAL",labkey.url.params$StartDate)));
    }
    if (!is.null(labkey.url.params$EndDate)) {
	    colFilter=rbind(colFilter,makeFilter(c("SampleFileId/AcquiredTime","DATE_LESS_THAN_OR_EQUAL",labkey.url.params$EndDate)));
    }
}

# call the selectRows function to get the data from the server
labkey.data <- labkey.selectRows(baseUrl=labkey.url.base,
                            folderPath=labkey.url.path,
                            schemaName="targetedms",
                            queryName="peptidechrominfo",
                            colSelect=colSelect,
                            colFilter=colFilter,
                            colSort="PeptideId/Sequence,-SampleFileId/AcquiredTime",
                            colNameOpt="rname",
                            maxRows=maxRows);

peptides = unique(labkey.data$peptideid_sequence)

# setup the png or pdf for the plot
if (!is.null(labkey.url.params$PdfOut)) {
    pdf(file="${pdfout:Levey-Jennings Trend Plot}", width=10, height=6);
} else {
    CairoPNG(filename="${imgout:Levey-Jennings Trend Plot}", width=810, height=300 * length(peptides));
    layout(matrix(1:length(peptides), length(peptides), 1));
    par(cex=1);
}

for (typeIndex in 1:length(peptides))
{
	dat = subset(labkey.data, peptideid_sequence == peptides[typeIndex]);

	mainTitle = peptides[typeIndex];

    dat$plottype_value = dat$retentiontime;
    dat$average = mean(dat$plottype_value);
    dat$stddev = sd(dat$plottype_value);
    dat$ptcolor = 1;


	# determine if the request is for log scale or not
	asLog = "";
	yAxisLabel = "Retention Time";
	if (!is.null(labkey.url.params$AsLog)) {
	    asLog = "y";
	    yAxisLabel = paste(yAxisLabel, "(log)", sep=" ");
	}

	# if there is no data for the selection, display a blank plot
	if(length(peptides[typeIndex]) > 0)
	{
	  # calculate the guide set ranges for each of the data points
	  dat$plus1stddev = dat$average + (1 * dat$stddev);
	  dat$plus2stddev = dat$average + (2 * dat$stddev);
	  dat$plus3stddev = dat$average + (3 * dat$stddev);
	  dat$minus1stddev = dat$average - (1 * dat$stddev);
	  dat$minus2stddev = dat$average - (2 * dat$stddev);
	  dat$minus3stddev = dat$average - (3 * dat$stddev);

	  # get the y axis min and max based on the data
	  if (any(!is.na(dat$plottype_value))) {
	      ymin = min(dat$plottype_value, na.rm=TRUE);
	      if (min(dat$minus3stddev, na.rm=TRUE) < ymin)
		    ymin = min(dat$minus3stddev, na.rm=TRUE);
	      ymax = max(dat$plottype_value, na.rm=TRUE);
	      if (max(dat$plus3stddev, na.rm=TRUE) > ymax)
            ymax = max(dat$plus3stddev, na.rm=TRUE);
	  } else {
	      ymin = 0;
	      ymax= 1;
	  }

	  # having a ymin and ymax that are the same messes up the legend position (issue 18507)
	  if (ymin == ymax) {
	      ymin = ymin - 1;
	      ymax = ymax + 1;
	  }

	  # if the plot is in log scale, make sure we don't have values <= 0
	  if (asLog == "y") {
	      if (ymin <= 0) { ymin = 1; }
	      dat$plus3stddev[dat$plus3stddev <= 0] = 1;
          dat$minus3stddev[dat$minus3stddev <= 0] = 1;
	  }

	  # set the sequence value for the records (in reverse order since they are sorted in DESC order)
	  dat$seq = length(dat$samplefileid_acquiredtime):1;
	  dat = dat[order(dat$seq),];

	  # determine the x-axis max
	  xmax= 10;
	  if(length(dat$plottype_value) > xmax)
	     xmax = length(dat$plottype_value);

	  # get the scaling factor to determine how many tick marks to show
	  tckFactor = ceiling(xmax/30);
	  # setup the tick marks and labels based on the scaling factor
	  xtcks = seq(1, xmax, by = tckFactor);

      # set the column labels to 'AcquiredTime'
      xlabels = as.character(dat$samplefileid_acquiredtime[xtcks])

	  # set some parameters and variables based on whether or not a legend is to be shown
	  par(mar=c(5.5,5,2,0.2));
	  mainTitleLine = 0.75;

	  # create an empty plotting area with a title
	  plot(NA, NA, type = c("b"), ylim=c(ymin,ymax), xlim=c(1,xmax), xlab="", ylab="", axes=F, log=asLog);
	  mtext(mainTitle, side=3, line=mainTitleLine, font=2, las=1, cex=1.2); 

	  # if creating a pdf, increase the line width and layout position offset
	  yLegendOffset = -0.5;
	  if (!is.null(labkey.url.params$PdfOut)) {
	    par(lwd=1.5);
	    yLegendOffset = -0.75;
	  }

	  # draw the guide set ranges for each of the records
	  for (i in 1:length(dat$retentiontime))
	  {
		# draw a vertial line to connect the min and max of the range
		lines(c(dat$seq[i], dat$seq[i]), c(dat$plus3stddev[i], dat$minus3stddev[i]), col='grey60', lty='solid');

		# draw dotted lines for the guide set ranges (3 stdDev above average)
		lines(c(dat$seq[i] - 0.3, dat$seq[i] + 0.3), c(dat$plus1stddev[i], dat$plus1stddev[i]), col='darkgreen', lty='dotted');
		lines(c(dat$seq[i] - 0.3, dat$seq[i] + 0.3), c(dat$plus2stddev[i], dat$plus2stddev[i]), col='blue', lty='dotted');
		lines(c(dat$seq[i] - 0.3, dat$seq[i] + 0.3), c(dat$plus3stddev[i], dat$plus3stddev[i]), col='red', lty='dotted');

		# draw dotted lines for the guide set ranges (3 stdDev below average)
		lines(c(dat$seq[i] - 0.3, dat$seq[i] + 0.3), c(dat$minus1stddev[i], dat$minus1stddev[i]), col='darkgreen', lty='dotted');
		lines(c(dat$seq[i] - 0.3, dat$seq[i] + 0.3), c(dat$minus2stddev[i], dat$minus2stddev[i]), col='blue', lty='dotted');
		lines(c(dat$seq[i] - 0.3, dat$seq[i] + 0.3), c(dat$minus3stddev[i], dat$minus3stddev[i]), col='red', lty='dotted');

		# draw a solid line at the guide set average
		lines(c(dat$seq[i] - 0.3, dat$seq[i] + 0.3), c(dat$average[i], dat$average[i]), col='grey60', lty='solid');
	  }

	  # draw points for the trend values for each record
	  points(dat$seq, dat$plottype_value, col=dat$ptcolor, pch=15);

	  # add the axis labels and tick marks
	  par(las=2);
	  axis(2, col="black");
	  mtext(yAxisLabel, side=2, line=4, las=0, font=2);
	  axis(1, col="black", at=xtcks, labels=FALSE, cex.axis=0.8);
	  staxlab(1, xtcks, xlabels, srt=25)
	  mtext("Assay", side=1, line=4, las=0, font=2);	  
	  box();

	} else {
	  par(mar=c(5.5,5,2,0.2));
	  plot(NA, NA, type = c("b"), ylim=c(1,1), xlim=c(1,30), xlab="", ylab="", axes=F, log=asLog);
	  mtext(mainTitle, side=3, line=0.75, font=2, las=1, cex=1.2);
	  text(15,1,"No Data Available for Selected Graph Parameters");
	  axis(1, at=seq(0,30,by=5), labels=matrix("",1,7), cex.axis=0.8);
	  mtext(yAxisLabel, side=2, line=4, las=0, font=2);
	  mtext("Assay", side=1, line=4, las=0, font=2);
	  box();
	}
}

# close the graphing device
dev.off();


