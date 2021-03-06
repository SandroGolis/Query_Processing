package InvertedIndexBuilder;

import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.mapreduce.Reducer;
import util.TermInfo;
import util.TermInfoArray;
import util.TextPair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/*
 * The reducer receives KEY=(Term, pageId)
 * VALUES = list of TermInfos for Term from all pages.
 * (as described in partitioner and grouper, pageId is redundant here and exists only for sorting purpose,
 * so, values will contain terms from all documents and not only the document with pageId)
 * The output is
 * Term,DF   <Offset_1, TF1, [idx_1,...,idx_TF1]> ,..., <Offset_DF, TF2, [idx_1,...,idx_TF2]>
 *
 */

public class IndexReducer extends Reducer<TextPair, TermInfo, TextPair, TermInfoArray> {
    private static int MIN_TF = 10;
    public void reduce(TextPair termAndPageId, Iterable<TermInfo> values,
                       Context context) throws IOException, InterruptedException {

        List<TermInfo> list = new ArrayList<TermInfo>();
        int TFsum = 0;
        for (TermInfo termInfo : values) {
            TFsum += termInfo.getTF();
            list.add(WritableUtils.clone(termInfo, context.getConfiguration()));
        }
        if (TFsum <= MIN_TF)
            return;

        Integer DF = list.size();
        TermInfoArray termsArray = new TermInfoArray(list.toArray(new TermInfo[]{}));

        TextPair key = new TextPair(termAndPageId.getFirst().toString(), Integer.toString(DF));
        context.write(key, termsArray);
    }
}