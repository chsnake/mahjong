package websiteschema.mpsegment.web.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import websiteschema.mpsegment.web.api.model.WordItem;
import websiteschema.mpsegment.web.api.model.dto.WordItemDto;
import websiteschema.mpsegment.web.ui.model.User;
import websiteschema.mpsegment.web.ui.service.UserService;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaQuery;
import java.util.List;

@Service
public class WordItemServiceImpl implements WordItemService {

    @PersistenceContext
    EntityManager em;

    @Autowired
    private UserService userService;

    @Override
    @Transactional
    public void add(WordItem wordItem) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        User currentUser = userService.getUserByEmail(userDetails.getUsername());
        wordItem.setUser(currentUser);
        em.persist(wordItem);
    }

    @Override
    @Transactional
    public void update(WordItemDto word) {
        WordItem wordItem = getById(word.id);
        WordItemUpdateRequestMerger merger = new WordItemUpdateRequestMerger();
        merger.merge(word).to(wordItem);
    }

    @Override
    public WordItem getById(int id) {
        return (WordItem)em.createQuery(
                "SELECT DISTINCT w From WordItem w " +
                        "LEFT OUTER JOIN FETCH w.pinyinSet pinyinSet " +
                        "LEFT OUTER JOIN FETCH w.conceptSet conceptSet " +
                        "LEFT OUTER JOIN FETCH w.wordFreqSet wordFreqSet " +
                        "LEFT OUTER JOIN FETCH conceptSet.partOfSpeech partOfSpeech " +
                        "LEFT OUTER JOIN FETCH wordFreqSet.partOfSpeech partOfSpeech2 " +
                        "LEFT OUTER JOIN FETCH w.user user " +
                        "WHERE w.id = :id")
                .setParameter("id", id)
                .getSingleResult();
    }

    @Override
    public List<WordItem> list() {
        CriteriaQuery<WordItem> c = em.getCriteriaBuilder().createQuery(WordItem.class);
        c.from(WordItem.class);
        return em.createQuery(c).getResultList();
    }

    @Override
    public List<WordItem> findAllByPinyin(String pinyin) {
        return em.createQuery(
                "SELECT DISTINCT w From WordItem w " +
                        "LEFT OUTER JOIN FETCH w.pinyinSet pinyinSet " +
                        "LEFT OUTER JOIN FETCH w.conceptSet conceptSet " +
                        "LEFT OUTER JOIN FETCH w.wordFreqSet wordFreqSet " +
                        "LEFT OUTER JOIN FETCH conceptSet.partOfSpeech partOfSpeech " +
                        "LEFT OUTER JOIN FETCH wordFreqSet.partOfSpeech partOfSpeech2 " +
                        "LEFT OUTER JOIN FETCH w.user user " +
                        "WHERE pinyinSet.name = :pinyin")
                .setParameter("pinyin", pinyin)
                .getResultList();
    }

    @Override
    public List<WordItem> findAllByWordHead(String wordHead) {
        return em.createQuery(
                "SELECT DISTINCT w From WordItem w " +
                        "LEFT OUTER JOIN FETCH w.pinyinSet pinyinSet " +
                        "LEFT OUTER JOIN FETCH w.conceptSet conceptSet " +
                        "LEFT OUTER JOIN FETCH w.wordFreqSet wordFreqSet " +
                        "LEFT OUTER JOIN FETCH conceptSet.partOfSpeech partOfSpeech " +
                        "LEFT OUTER JOIN FETCH wordFreqSet.partOfSpeech partOfSpeech2 " +
                        "LEFT OUTER JOIN FETCH w.user user " +
                        "WHERE w.name like :wordHead")
                .setParameter("wordHead", wordHead.trim() + "%")
                .getResultList();
    }

    @Override
    public void remove(int id) {
        WordItem person = getById(id);
        if (null != person) {
            em.remove(person);
        }
    }
}
