package com.devprofile.DevProfile.controller;

import com.devprofile.DevProfile.component.JwtProvider;
import com.devprofile.DevProfile.entity.UserEntity;
import com.devprofile.DevProfile.repository.UserRepository;
import com.devprofile.DevProfile.service.GitLoginService;
import com.devprofile.DevProfile.service.gpt.GPTService;
import com.devprofile.DevProfile.service.graphql.GraphUserService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import reactor.core.publisher.Mono;

import java.io.IOException;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);


    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final JwtProvider jwtProvider;

    @Autowired
    private final GitLoginService gitLoginService;

    @Autowired
    private final GraphUserService userService;

    @Autowired
    private final GPTService gptService;



    @GetMapping("/main")
    public Mono<Void> main(@RequestHeader String Authorization) throws IOException {


        jwtProvider.validateToken(Authorization);
        String primaryId = jwtProvider.getIdFromJWT(Authorization);
        log.info(primaryId);
        UserEntity user = userRepository.findById(Integer.parseInt(primaryId)).orElseThrow();
        System.out.println("accessToken = " + user.getGitHubToken());

        return userService.UserSaves(user);
    }

    @GetMapping("/test/gpt")
    public void testGpt(){
        String patch = "@@ -67,7 +67,6 @@ vm_alloc_page_with_initializer (enum vm_type type, void *upage, bool writable,\n" +
                " \n" +
                " \t\t// 가상 메모리 타입에 따라 초기화자 함수 포인터를 설정합니다.\n" +
                " \t\tint ty = VM_TYPE (type);\n" +
                "-\t\tbool seg = (VM_IS_CODE(type) == VM_MARKER_CODE);\n" +
                " \t\tbool (*initializer)(struct page *, enum vm_type, void *);\n" +
                " \t\t\n" +
                " \t\tswitch(ty){\n" +
                "@@ -82,8 +81,6 @@ vm_alloc_page_with_initializer (enum vm_type type, void *upage, bool writable,\n" +
                "         uninit_new(page, upage, init, type, aux, initializer);\n" +
                " \n" +
                " \t\tpage->writable = writable;\n" +
                "-\t\tpage->seg = seg;\n" +
                "-\t\tpage->curr = thread_current();\n" +
                " \t\tpage->swap =false;\n" +
                " \t\t // 페이지를 보조 페이지 테이블에 삽입합니다.\n" +
                "         if (!spt_insert_page(spt, page)) {\n" +
                "@@ -217,10 +214,10 @@ vm_stack_growth (void *addr UNUSED) {\n" +
                " \tstruct page* page = spt_find_page(&thread_current()->spt, page_addr);\n" +
                " \twhile(page == NULL){\n" +
                " \t\tvm_alloc_page(VM_ANON, page_addr, true);\n" +
                "-\t\tvm_claim_page(page_addr);\n" +
                " \t\tpage_addr+= PGSIZE;\n" +
                " \t\tpage = spt_find_page(&thread_current()->spt, page_addr);\n" +
                " \t}\n" +
                "+\tvm_claim_page(pg_round_down(addr));\n" +
                " }\n" +
                " \n" +
                " /* Handle the fault on write_protected page */\n" +
                "@@ -343,29 +340,23 @@ bool supplemental_page_table_copy(struct supplemental_page_table *dst, struct su\n" +
                " \n" +
                "     hash_first(&i, src_hash);\n" +
                "     while (hash_next(&i)) {\n" +
                "-        struct page *src_page = hash_entry(hash_cur(&i), struct page, hash_elem);\n" +
                "-        \n" +
                "+        struct page *src_page = hash_entry(hash_cur(&i), struct page, hash_elem);   \n" +
                "         // Allocate and claim the page in dst\n" +
                " \t\tenum vm_type type = src_page->operations->type;\n" +
                " \t\tif(type== VM_UNINIT){\n" +
                " \t\t\tstruct uninit_page *uninit_page = &src_page->uninit;\n" +
                " \t\t\tstruct file_loader* file_loader = (struct file_loader*)uninit_page->aux;\n" +
                " \t\t\tstruct file_loader* new_file_loader = malloc(sizeof(struct file_loader));\n" +
                "-\t\t\tmemcpy(new_file_loader, uninit_page->aux, sizeof(struct file_loader));\n" +
                "-\t\t\tnew_file_loader -> file = file_duplicate(file_loader->file);\n" +
                "-\t\t\t//writable true\n" +
                "-\t\t\tvm_alloc_page_with_initializer(uninit_page->type,src_page->va,true,uninit_page->init,new_file_loader);\n" +
                "-        \tvm_claim_page(src_page->va);\n" +
                "+\t\t\tmemcpy(new_file_loader, file_loader, sizeof(struct file_loader));\n" +
                "+\t\t\tnew_file_loader -> file = file_reopen(file_loader->file);\n" +
                "+\t\t\tvm_alloc_page_with_initializer(uninit_page->type,src_page->va,src_page->writable,uninit_page->init,new_file_loader);\n" +
                " \t\t}else{\n" +
                "         \tvm_alloc_page(src_page->operations->type, src_page->va, true);\n" +
                "         \tvm_claim_page(src_page->va);\n" +
                "         \tmemcpy(src_page->va, src_page->frame->kva,PGSIZE);\n" +
                " \t\t}\n" +
                "-\n" +
                "         // Insert the copied page into dst's supplemental page table\n" +
                "-\n" +
                "     }\n" +
                "-    \n" +
                "     return true;\n" +
                " }\n" +
                " \n" +
                "@@ -382,8 +373,8 @@ supplemental_page_table_kill (struct supplemental_page_table *spt ) {\n" +
                " \n" +
                " void\n" +
                " supplemental_page_table_free (struct supplemental_page_table *spt ) {\n" +
                "-\t/* TODO: Destroy all the supplemental_page_table hold by thread and\n" +
                "-\t * TODO: writeback all the modified contents to the storage. */\n" +
                "+\t//TODO: Destroy all the supplemental_page_table hold by thread and\n" +
                "+\t //TODO: writeback all the modified contents to the storage. \n" +
                " \thash_destroy(&spt->pages, hash_action_destroy);\n" +
                " }\n" +
                " \n" +
                "@@ -394,9 +385,11 @@ void hash_action_destroy(struct hash_elem* hash_elem_, void *aux){\n" +
                " \t\tif (VM_TYPE(page->operations->type) == VM_FILE && !page->swap) {\n" +
                "         \tstruct file_page *file_page = &page->file;\n" +
                " \t\t\tstruct file* file = file_page->file; // 파일 포인터 갱신\n" +
                "+\t\t\t//!!TODO: dirty bit check\n" +
                " \t\t\tif(file)\n" +
                " \t\t\t\tfile_write_at(file, page->frame->kva, file_page->read_bytes, file_page->ofs);\n" +
                " \t\t}\n" +
                "+\t\t//!!TODO: mmap list 할당해제, file close\n" +
                " \t\tif(page->frame != NULL){\n" +
                " \t\t\tfree_frame(page->frame);\n" +
                " \t\t\tpage->frame = NULL;";
        System.out.println(gptService.generateKeyword(patch));
    }
}

