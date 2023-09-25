# Table of Contents

- [General info](#general-info)
- [Requirements](#requirements)
- [How to run feTruth](#how-to-run-fetruth)
- [Dataset](#dataset)

# General info

feTruth is a tool written in Python that can detect feature envy methods in a Java project.

# Requirements

- Python == 3.6
- Tensorflow == 2.4.0
- Keras == 2.4.3
- Numpy == 1.19.5
- Gensim == 3.8.0
- Scikit-learn == 0.24.2

You can setup the requirements via any of the following commands:

1. requirements.txt

   `pip install -r requiremetns.txt`

2. setup.py

   `python setup.py` or `./setup.py`

3. conda environment

   `conda env create -f fetruth.yaml`

# How to run feTruth

1. **Clone feTruth to your local file system**

   `git clone https://github.com/lyoubo/feTruth.git`


2. **Install dependencies**

   `cd feTruth/library`

   `pip install -r requirements.txt`


3. **Download the two large files requested by the word2vec model**

   Download [`new_model.bin.trainables.syn1neg.npy`](https://doi.org/10.5281/zenodo.5749111) and [`new_model.bin.wv.vectors.npy`](https://doi.org/10.5281/zenodo.5749111) from Zenodo and put them into folder `word2vec`. And now, the folder `word2vec` should contain 3 files (including one copy automatically from this repository).


4. **Run feTruth**

   `cd feTruth/implementation/`

   `python feTruth.py -a E:/jsoup`

In this case, the identified feature envy methods are available at `jsoup.txt` in folder `output`:

    Refactoring Type	Source Method	Target Class
    Move Method	org.jsoup.helper.DataUtil::mimeBoundary():java.lang.String 	 org.jsoup.internal.StringUtil
    Move Method	org.jsoup.helper.HttpConnection.Response::serialiseRequestUrl(org.jsoup.Connection.Request):void 	 org.jsoup.internal.StringUtil
    Move Method	org.jsoup.Jsoup::parse(java.io.File, java.lang.String, java.lang.String):org.jsoup.nodes.Document 	 org.jsoup.helper.DataUtil
    Move Method	org.jsoup.Jsoup::parse(java.io.File, java.lang.String):org.jsoup.nodes.Document 	 org.jsoup.helper.DataUtil
    Move Method	org.jsoup.Jsoup::parse(java.io.InputStream, java.lang.String, java.lang.String):org.jsoup.nodes.Document 	 org.jsoup.helper.DataUtil
    Move Method	org.jsoup.Jsoup::parse(java.io.InputStream, java.lang.String, java.lang.String, org.jsoup.parser.Parser):org.jsoup.nodes.Document 	 org.jsoup.helper.DataUtil
    Move Method	org.jsoup.Jsoup::parseBodyFragment(java.lang.String):org.jsoup.nodes.Document 	 org.jsoup.parser.Parser
    Move Method	org.jsoup.nodes.Attribute::html(java.lang.String, java.lang.String, java.lang.Appendable, org.jsoup.nodes.Document.OutputSettings):void 	 org.jsoup.nodes.Attributes
    Move Method	org.jsoup.nodes.Element::appendNormalisedText(java.lang.StringBuilder, org.jsoup.nodes.TextNode):void 	 org.jsoup.internal.StringUtil
    Move Method	org.jsoup.nodes.Entities::escape(java.lang.String, org.jsoup.nodes.Document.OutputSettings):java.lang.String 	 org.jsoup.internal.StringUtil
    Move Method	org.jsoup.nodes.Entities::escape(java.lang.Appendable, java.lang.String, org.jsoup.nodes.Document.OutputSettings, boolean, boolean, boolean):void 	 org.jsoup.internal.StringUtil
    Move Method	org.jsoup.parser.HtmlTreeBuilderState::handleRcData(org.jsoup.parser.Token.StartTag, org.jsoup.parser.HtmlTreeBuilder):void 	 org.jsoup.parser.TreeBuilder
    Move Method	org.jsoup.parser.Parser::parseBodyFragment(java.lang.String, java.lang.String):org.jsoup.nodes.Document 	 org.jsoup.nodes.Document
    Move Method	org.jsoup.parser.TokeniserState::readEndTag(org.jsoup.parser.Tokeniser, org.jsoup.parser.CharacterReader, org.jsoup.parser.TokeniserState, org.jsoup.parser.TokeniserState):void 	 org.jsoup.parser.CharacterReader
    Move Method	org.jsoup.parser.TokenQueue::unescape(java.lang.String):java.lang.String 	 org.jsoup.internal.StringUtil

# Dataset

The dataset we used to train feTruth includes 33,419 positive samples and 33,419 negative samples, is available in the following links:

- [training data](dataset/training_data/)

#### &emsp;JSON property descriptions

&emsp;<font size=2>**id**: identity of the moved method</font>  
&emsp;<font size=2>**project_name**: project name</font>    
&emsp;<font size=2>**commit_id**: Git commit ID</font>    
&emsp;<font size=2>**source_class_name**: class name that the moved method belonged to before the move</font>    
&emsp;<font size=2>**method_name**: name of the moved method</font>    
&emsp;<font size=2>**source_dist**: Jaccard distance between the moved method and the source class</font>    
&emsp;<font size=2>**source_cbmc**: coupling between the moved method and the source class</font>    
&emsp;<font size=2>**source_mcmc**: message passing coupling between the moved method and the source class</font>    
&emsp;<font size=2>**target_class_name**: class name that the moved method belonges to after the move</font>    
&emsp;<font size=2>**target_dist**: Jaccard distance between the moved method and the target class</font>    
&emsp;<font size=2>**target_cbmc**: coupling between the moved method and the target class</font>    
&emsp;<font size=2>**target_mcmc**: message passing coupling between the moved method and the target class</font>    
&emsp;<font size=2>**refactoring_id**: identity of the refactoring operation</font>

The source code used by feTruth for data generation is available in the following links:

- [data generation](dataset/data_generation)
