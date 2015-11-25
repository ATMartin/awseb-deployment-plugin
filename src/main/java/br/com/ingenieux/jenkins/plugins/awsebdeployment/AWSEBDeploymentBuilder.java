package br.com.ingenieux.jenkins.plugins.awsebdeployment;

/*
 * #%L
 * AWS Elastic Beanstalk Deployment Plugin
 * %%
 * Copyright (C) 2013 ingenieux Labs
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.util.CopyOnWriteList;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * AWS Elastic Beanstalk Deployment
 */
@SuppressWarnings({ "unchecked" })
public class AWSEBDeploymentBuilder extends Builder implements BuildStep {
    @DataBoundConstructor
	public AWSEBDeploymentBuilder(String credentialsName,
			String awsRegion,
			String applicationName, String environmentName, String bucketName,
			String keyPrefix, String versionLabelFormat, String rootObject, boolean zeroDowntime,
			String includes, String excludes) {
		super();
        this.credentialsName = credentialsName;
		this.awsRegion = awsRegion;
		this.applicationName = applicationName;
		this.environmentName = environmentName;
		this.bucketName = bucketName;
		this.keyPrefix = keyPrefix;
		this.versionLabelFormat = versionLabelFormat;
		this.rootObject = rootObject;
        this.zeroDowntime = zeroDowntime;
        this.includes = includes;
		this.excludes = excludes;
	}

	/**
	 * Credentials name
	 */
	private String credentialsName;
    
    /**
     * AWS credentials name
     */
    public String getCredentialsName() {
        return credentialsName;
    }

    /**
	 * AWS Region
	 */
	private String awsRegion;

	public String getAwsRegion() {
		return awsRegion;
	}

	public void setAwsRegion(String awsRegion) {
		this.awsRegion = awsRegion;
	}

	/**
	 * Application Name
	 */
	private String applicationName;

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	/**
	 * Environment Name
	 */
	private String environmentName;

	public String getEnvironmentName() {
		return environmentName;
	}

	public void setEnvironmentName(String environmentName) {
		this.environmentName = environmentName;
	}

	/**
	 * Bucket Name
	 */
	private String bucketName;

	public String getBucketName() {
		return bucketName;
	}

	public void setBucketName(String bucketName) {
		this.bucketName = bucketName;
	}

	/**
	 * Key Format
	 */
	private String keyPrefix;

	public String getKeyPrefix() {
		return keyPrefix;
	}

	public void setKeyPrefix(String keyFormat) {
		this.keyPrefix = keyFormat;
	}

	private String versionLabelFormat;

	public String getVersionLabelFormat() {
		return versionLabelFormat;
	}

	public void setVersionLabelFormat(String versionLabelFormat) {
		this.versionLabelFormat = versionLabelFormat;
	}

	private String rootObject;

	public String getRootObject() {
		return rootObject;
	}

	public void setRootObject(String rootDirectory) {
		this.rootObject = rootDirectory;
	}

	private String includes;

	public String getIncludes() {
		return includes;
	}

	public void setIncludes(String includes) {
		this.includes = includes;
	}

	private String excludes;

	public String getExcludes() {
		return excludes;
	}

	public void setExcludes(String excludes) {
		this.excludes = excludes;
	}

    private boolean zeroDowntime;

    public void setCredentialsName(String credentialsName) {
        this.credentialsName = credentialsName;
    }

    public boolean isZeroDowntime() {
        return zeroDowntime;
    }

    public void setZeroDowntime(boolean zeroDowntime) {
        this.zeroDowntime = zeroDowntime;
    }

    @Override
	public boolean perform(AbstractBuild<?, ?> build, Launcher launcher,
			BuildListener listener) {
		try {
			DeployerContext deployerContext = new DeployerContext(this, build, launcher, listener);

			DeployerChain deployerChain = new DeployerChain(deployerContext);

			deployerChain.perform();

			return true;
		} catch (Exception exc) {
			throw new RuntimeException(exc);
		}
	}

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @SuppressWarnings("rawtypes")
	@Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    public String getAwsAccessKeyId() {
        return getCredentials().getAwsAccessKeyId();
    }

    public String getAwsSecretSharedKey() {
        return getCredentials().getAwsSecretSharedKey();
    }
    
    protected DescriptorImpl.AwsCredentials getCredentials() {
        DescriptorImpl.AwsCredentials[] credentials = getDescriptor().getCredentials();

        if (credentialsName == null && credentials.length > 0)
            // default
            return credentials[0];

        for (DescriptorImpl.AwsCredentials credential : credentials) {
            if (credential.getName().equals(credentialsName))
                return credential;
        }
        
        return null;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        public static class AwsCredentials {
            private final String name;
            private final String awsAccessKeyId;
            private final String awsSecretSharedKey;

            public String getName() {
                return name;
            }

            public String getAwsAccessKeyId() {
                return awsAccessKeyId;
            }

            public String getAwsSecretSharedKey() {
                return awsSecretSharedKey;
            }

            public AwsCredentials() {
                name = null;
                awsAccessKeyId = null;
                awsSecretSharedKey = null;
            }

            @DataBoundConstructor
            public AwsCredentials(String name, String awsAccessKeyId, String awsSecretSharedKey) {
                this.name = name;
                this.awsAccessKeyId = awsAccessKeyId;
                this.awsSecretSharedKey = awsSecretSharedKey;
            }
        }
        
        private final CopyOnWriteList<AwsCredentials> credentials = new CopyOnWriteList<AwsCredentials>();

        public AwsCredentials[] getCredentials() {
            return credentials.toArray(new AwsCredentials[0]);
        }
        
        public DescriptorImpl() {
            load();
        }

		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        public String getDisplayName() {
            return "AWS Elastic Beanstalk";
        }

        @Override
        public boolean configure(StaplerRequest req, net.sf.json.JSONObject json) throws FormException {
            credentials.replaceBy(req.bindParametersToList(AwsCredentials.class, "credential."));
            save();
            return true;
        }
    }
}
