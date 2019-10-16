Please refer to "Assignment2.pdf" for the spec of this homework
Read "extra_credit_weiming_liu.pdf" about two algorithm developed by me

How to Compile:
Use command "javac HW2_weiming_liu.java" to compile

How to run:
Use command "java -Xmx256m HW2_weiming_liu input_image_file n m" to execute.
e.x. java -Xmx256m HW2_weiming_liu sample1_256x256.rgb 8 2

n = number of vectors in codebook (only n colors can be used in compressed image)
m = 1: use uniform quantization.
m = 2: use peak searching algorithm. (Please refer to "extra_credit_weiming_liu.pdf")
M = 3: use weighted count algorithm. (Please refer to "extra_credit_weiming_liu.pdf")

Five windows will pop up. They are original image, compressed image, difference in R channel, difference in G channel, and difference in B channel.

p.s. Please don't choose big n (n>32) when m = 3. It may take a lot of time to run.
