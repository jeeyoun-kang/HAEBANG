package com.haebang.haebang.controller;

import com.haebang.haebang.entity.Bookmark;
import com.haebang.haebang.entity.Item;
import com.haebang.haebang.entity.Member;
import com.haebang.haebang.repository.BookmarkRepository;
import com.haebang.haebang.repository.ItemRepository;
import com.haebang.haebang.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("api")
public class BookmarkController {

    private final MemberRepository memberRepository;
    private final BookmarkRepository bookmarkRepository;
    private final ItemRepository itemRepository;

    @GetMapping("bookmark")
    ResponseEntity getBookmarkList(@CookieValue(name = "username")String username){
// 로그인 된 사람만, jwt 토큰 검사 필요
        Member member = memberRepository.findByUsername( username ).orElseThrow();
        List<Bookmark> bookmarkList = bookmarkRepository.findBookmarkByMember(member);

        List<Long> idList = bookmarkList.stream().map(bookmark -> bookmark.getItem().getId()) // Bookmark 객체에서 Item의 id 필드를 추출하여 Long으로 매핑
                .collect(Collectors.toList());
        // 으아아아ㅏ 내가 한 북마크 목록 어떻게 불러와야 하리 모르겠어어어어ㅓㅓㅓ
        // bookmark에 있는 item 이그노어 풀면 재귀걸리고
        // member에 해당하는 item 가져오는 건 저것 밖에 없고
        List<Item> items = itemRepository.findAllById(idList);
        return new ResponseEntity(items, HttpStatus.OK);
    }

    @PostMapping("bookmark/{item_id}")
    ResponseEntity doBookmark(@PathVariable("item_id") Long itemId, Authentication authentication){
        // 로그인 된 사람만, jwt 토큰 검사 필요
        log.info("북마크 post 요청");
        Member member = memberRepository.findByUsername( authentication.getName()).orElseThrow();
        Item item = itemRepository.findById(itemId).orElseThrow();
        Bookmark bookmark = Bookmark.builder()
                .member(member)
                .item(item)
                .build();
        bookmarkRepository.save(bookmark);
        return new ResponseEntity( "북마크 완료", HttpStatus.OK);
    }

    @DeleteMapping("bookmark/{item_id}")
    ResponseEntity undoBookmark(@PathVariable("item_id") Long itemId, Authentication authentication){
        Member member = memberRepository.findByUsername( authentication.getName()).orElseThrow();
        Item item = itemRepository.findById(itemId).orElseThrow();

        Bookmark bookmark = Bookmark.builder()
                .member(member)
                .item(item)
                .build();
        bookmarkRepository.delete(bookmark);

        return new ResponseEntity("북마크 취소", HttpStatus.OK);
    }
}
