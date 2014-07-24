package org.ansj.splitWord.analysis;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

import love.cq.domain.Forest;

import org.ansj.app.crf.SplitWord;
import org.ansj.dic.LearnTool;
import org.ansj.domain.NewWord;
import org.ansj.domain.Term;
import org.ansj.library.InitDictionary;
import org.ansj.library.NatureLibrary;
import org.ansj.recognition.AsianPersonRecognition;
import org.ansj.recognition.NatureRecognition;
import org.ansj.recognition.NewWordRecognition;
import org.ansj.recognition.NumRecognition;
import org.ansj.recognition.UserDefineRecognition;
import org.ansj.splitWord.Analysis;
import org.ansj.util.Graph;
import org.ansj.util.MyStaticValue;
import org.ansj.util.WordAlert;

/**
 * 自然语言分词,具有未登录词发现功能。建议在自然语言理解中用。搜索中不要用
 * 
 * @author wanggang
 *
 */
public class NlpAnalysis extends Analysis {

	private LearnTool learn = null;

	private static final SplitWord DEFAULT_SLITWORD = MyStaticValue.getCRFSplitWord();

	@Override
	protected List<Term> getResult(final Graph graph) {
		Merger merger = new Merger() {
			@Override
			public List<Term> merger() {
				graph.walkPath();

				// 数字发现
				if (graph.hasNum) {
					NumRecognition.recognition(graph.terms);
				}

				// 词性标注
				List<Term> result = getResult();
				new NatureRecognition(result).recognition();

				if (learn == null) {
					learn = new LearnTool();
				}
				learn.learn(graph);

				// 通过crf分词
				List<String> words = DEFAULT_SLITWORD.cut(graph.chars);

				for (String word : words) {
					if (word.length() < 2 || InitDictionary.isInSystemDic(word) || WordAlert.isRuleWord(word)) {
						continue;
					}
					learn.addTerm(new NewWord(word, NatureLibrary.getNature("nw"), -word.length()));
				}

				// 用户自定义词典的识别
				new UserDefineRecognition(graph.terms, forests).recognition();
				graph.walkPathByScore();

				// 进行新词发现
				new NewWordRecognition(graph.terms, learn).recognition();
				graph.walkPathByScore();

				// 修复人名左右连接
				AsianPersonRecognition.nameAmbiguity(graph.terms);

				// 优化后重新获得最优路径
				result = getResult();

				//激活辞典
				for (Term term : result) {
					learn.active(term.getName());
				}

				setRealName(graph, result);

				return result;
			}

			private List<Term> getResult() {
				List<Term> result = new ArrayList<Term>();
				int length = graph.terms.length - 1;
				for (int i = 0; i < length; i++) {
					if (graph.terms[i] == null) {
						continue;
					}
					result.add(graph.terms[i]);
				}
				return result;
			}
		};
		return merger.merger();
	}

	private NlpAnalysis() {
		//
	}

	/**
	 * 用户自己定义的词典
	 * 
	 * @param forest
	 */

	public NlpAnalysis(Forest... forests) {
		this.forests = forests;
	}

	public NlpAnalysis(LearnTool learn, Forest... forests) {
		this.forests = forests;
		this.learn = learn;
	}

	public NlpAnalysis(BufferedReader reader, Forest... forests) {
		this.forests = forests;
		super.resetContent(reader);
	}

	public NlpAnalysis(BufferedReader reader, LearnTool learn, Forest... forests) {
		this.forests = forests;
		this.learn = learn;
		super.resetContent(reader);
	}

	public static List<Term> parse(String str) {
		return new NlpAnalysis().parseStr(str);
	}

	public static List<Term> parse(String str, Forest... forests) {
		return new NlpAnalysis(forests).parseStr(str);
	}

	public static List<Term> parse(String str, LearnTool learn, Forest... forests) {
		return new NlpAnalysis(learn, forests).parseStr(str);
	}

}
