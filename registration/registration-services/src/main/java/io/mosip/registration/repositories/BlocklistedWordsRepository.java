package io.mosip.registration.repositories;

import java.util.List;

import io.mosip.kernel.core.dataaccess.spi.repository.BaseRepository;
import io.mosip.registration.entity.BlocklistedWords;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Query;

/**
 * Repository for Blocklisted words.
 * 
 * @author Sreekar Chukka
 * @since 1.0.0
 */
public interface BlocklistedWordsRepository extends BaseRepository<BlocklistedWords, String> {

	List<BlocklistedWords> findBlockListedWordsByIsActiveTrueAndLangCode(String langCode);
	List<BlocklistedWords> findBlockListedWordsByIsActiveTrue();

	@Query("select lower(word) from BlocklistedWords where isActive = true")
	List<String> findAllActiveBlockListedWords();
}
