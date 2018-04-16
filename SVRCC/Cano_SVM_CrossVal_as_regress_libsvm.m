function [] = Cano_SVM_CrossVal_as_regress_libsvm(both, kernel, tube_e)
close all
% Alberto Cano's problem - rbf_nn for multi-target regression
% Approximations of mapping m dimensional vectors x_i, into n dimensional vectors y_i
%kernel -> linear = 0, Polynomial = 1,	Gaussian = 2, parameters = Degree of Poly, or StDev sigma of Gaussian, g = gamma = 1 
%load D:\a_Vojo\aavkec001_matlab_Aavk_i_vojo\aaVojiniMATLABovi\a_a_datasetsVK\erf1
%load D:\a_Vojo\aavkec001_matlab_Aavk_i_vojo\aaVojiniMATLABovi\edmALL2
load edmALL2
[mtr dim] = size(Xtrain);	[mts dim] = size(Xtest);
g=1;	ss = 3;	e = tube_e;	t = kernel;
X=[Xtrain;Xtest];	Xs = X;
%Xs=scale_vk(X,2);	Xs=scale(X);% IF you don't want to scale i.e. normalize data just cancel this line
Xtrain0 = Xs(1:mtr,:);	Xtest0 = Xs(mtr+1:end,:); 

%indtr=find(Ytrain==0);Ytrain(indtr)=-1; indts=find(Ytest==0);Ytest(indts)=-1;

if both == 1,			Ytrain0=Ytrain(:,1);	Ytest0=Ytest(:,1);
elseif both == 2,	Ytrain0=Ytrain(:,2);	Ytest0=Ytest(:,2);
else,								Ytrain0=Ytrain;			Ytest0=Ytest;
end
clear Xtrain Xtest Ytrain  Ytest

Correl_Coeff_tr = corrcoef(Ytrain0)
Correl_Coeff_ts = corrcoef(Ytest0)
pause(1)

C0 = [0.1 0.5 1 5 1e1 1e2 1e3]
%s0 = [0.5 1 2.5 5 10 25 50 100]
gamma0 = [0.5 1 2 5 10 25 50]

examplestr = [138 138 138 138 139 139 139 139 139 139];
sumtr(1)=138; for i = 2:10,sumtr(i)=sumtr(i-1)+examplestr(i);end

examplests=[16 16 16 16 15 15 15 15 15 15]; 
sumts(1)=16;  for i = 2:10,	sumts(i)=sumts(i-1)+examplests(i);end
fold = 10;

for l = 1:10
	index = crossvalind('Kfold',examplestr(l),fold);

	for i = 1:length(C0)
		C = C0(i);
		for j = 1:length(gamma0)
			param = gamma0(j);
			errors=0;
				for kf = 1:fold 
	        if fold ~= 1;		test = (index == kf);      train = ~test;
	        else            test = (index == kf);      train = test; end
	        if l == 1; Xtrain = Xtrain0(1:sumtr(1),:);						Ytrain = Ytrain0(1:sumtr(1),:);
	        else			 Xtrain = Xtrain0(sumtr(l-1)+1:sumtr(l),:);	Ytrain = Ytrain0(sumtr(l-1)+1:sumtr(l),:);
	        end	        
	        Xtr = Xtrain(train,:);      Ytr = Ytrain(train,:);
	        Xts = Xtrain(test,:);    		Yts = Ytrain(test,:);
	        % Solving Dual Lagrangian for ALPHAs & CrossValidation test Data
					%[Ytestapprox] = libsvm_CORE_in_matlab(Xtr,Ytr,Xts,Yts,kernel,C,s,ss,e,g);
							if kernel == 0
			
							elseif kernel == 1
								s=3; % no mixing numbers with strings below, this is why s should be defined here and below it has to be  num2str(s) 
								model = svmtrain([],Ytr,Xtr, ['-p',num2str(e),'-s', num2str(s),'-t',num2str(t),'-c ',num2str(C),'-d',num2str(param)]);
							else
								s=3; % no mixing numbers with strings below, this is why s should be defined here and below it has to be  num2str(s) 
								model = svmtrain([],Ytr,Xtr, [ ' -p ',num2str(e),' -s ', num2str(s), ' -t ',num2str(t),' -c ',num2str(C),' -g ',num2str(param)]);
							end
	
						[Ytestapprox] = svmpredict(Yts,Xts, model); % test the training data
					Ytrmean = mean(Ytr);YtsM = ones(size(Yts))*diag(Ytrmean);
					err = sqrt( sum(sum((Ytestapprox-Yts).^2)) / sum(sum((YtsM-Yts).^2))  );
					errors = errors + err;
					clear Xtr Ytr Xts Yts Ytrmean YtsM Ytestapprox
				end % for kf = 1:fold
			Errors(i,j) = 100*errors/fold;
		end	% for i = 1:length(K)
	end % for j = 1:length(S)
	%Errors,	C0,		gamma0
	[min_error_rate, i1,i2] = min_array(Errors);		clear Errors
	MIN_error_rate = min_error_rate;
	C_best=C0(i1);	Gamma_best=gamma0(i2);
	MIN_error_rate__C_best__Gamma_best = [MIN_error_rate C_best Gamma_best]
	
	C = C_best;	param = Gamma_best;
	% Solving Dual Lagrangian for ALPHAs & Test Data
	% Test Data
	if l == 1;  Xtest = Xtest0(1:sumts(1),:);	Ytest = Ytest0(1:sumts(1),:);
	else				Xtest = Xtest0(sumts(l-1)+1:sumts(l),:);	Ytest = Ytest0(sumts(l-1)+1:sumts(l),:);
	end
	%[Ytestapprox] = libsvm_CORE_in_matlab(Xtrain,Ytrain,Xtest,Ytest,kernel,C,s,ss,e,g)
	%plot(Ytest,'ro'),hold on,plot(Ytestapprox,'bx'),pause,clf
	if kernel == 0

	elseif kernel == 1
		s=3; % no mixing numbers with strings below, this is why s should be defined here and below it has to be  num2str(s) 
		model = svmtrain([],Ytrain,Xtrain, ['-p',num2str(e),'-s', num2str(s),'-t',num2str(t),'-c ',num2str(C),'-d',num2str(param)]);
	else
		s=3; % no mixing numbers with strings below, this is why s should be defined here and below it has to be  num2str(s) 
		model = svmtrain([],Ytrain,Xtrain, [ ' -p ',num2str(e),' -s ', num2str(s), ' -t ',num2str(t),' -c ',num2str(C),' -g ',num2str(param)]);
	end
	
	[Ytestapprox] = svmpredict(Ytest,Xtest, model); % test the training data
	Ytrmean = mean(Ytrain);YtsM = ones(size(Ytest))*diag(Ytrmean);
	err = sqrt( sum(sum((Ytestapprox-Ytest).^2)) / sum(sum((YtsM-Ytest).^2))  );
	for i = 1:size(Ytrain,2)
		Errors_Test(l,i) = 100*sqrt( sum((Ytestapprox(:,i)-Ytest(:,i)).^2) ) / sqrt( sum((YtsM(:,i)-Ytest(:,i)).^2)  );
	end
	Errors_Test(l,size(Ytrain,2)+1) = mean(Errors_Test(l,1:size(Ytrain,2))), pause(0.5)
	clear Xtrain Ytrain Xtest Ytest Ytrmean YtsM Ytestapprox
end

Mean_Error_Test = mean(Errors_Test)
both__kernel__fold__tube_e = [both kernel fold tube_e]

clear




