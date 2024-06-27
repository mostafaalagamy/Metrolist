import com.malopieds.kugou.KuGou
import com.malopieds.kugou.KuGou.generateKeyword
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class Test {
    @Test
    fun test() = runBlocking {
        val candidates = KuGou.getLyricsCandidate(generateKeyword("千年以後 (After A Thousand Years)", "陳零九"), 285)
        assertTrue(candidates != null)
        assertTrue(KuGou.getLyrics("楊丞琳", "點水", 259).isSuccess)
    }
}
