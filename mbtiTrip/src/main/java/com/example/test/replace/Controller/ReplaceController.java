package com.example.test.replace.Controller;

import java.security.Principal;
import java.util.Iterator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;



import com.example.test.POST.Controller.PageDTO;
import com.example.test.User.DTO.UserDTO;
import com.example.test.User.Service.UserService;
import com.example.test.paging.Criteria;
import com.example.test.replace.ReplaceForm;
import com.example.test.replace.DTO.ReplaceCategoryDTO;
import com.example.test.replace.DTO.ReplaceDTO;
import com.example.test.replace.Service.ReplaceCategoryService;
import com.example.test.replace.Service.ReplaceService;


import jakarta.validation.Valid;

@RequestMapping("/replace/*")
@Controller
public class ReplaceController {

	@Autowired
	ReplaceService rpService;
	
	@Autowired
	UserService userService;
	
	@Autowired
	ReplaceCategoryService rpCategoryService;
	
	@RequestMapping("/list")
	public ModelAndView List(ModelAndView mv, Criteria cri) throws Exception {

	    PageDTO pageMaker = new PageDTO();
	    pageMaker.setCri(cri); //page, perpagenum 셋팅
	    pageMaker.setTotalCount(rpService.listCount(cri)); //총 게시글 수 셋팅

	    //View에 페이징 처리를 위한 조건 및 그에 맞는 게시판 리스트 전송
	    mv.addObject("pageMaker", pageMaker);
	    mv.addObject("data", rpService.list(cri)); //현재페이지에 표시할 게시글 목록 가져옴

	    mv.setViewName("replace_list");

	    return mv;
	    }
	
	
    @RequestMapping(value = "/detail/{id}")
    public String detail(Model model, @PathVariable("id") Integer replaceID) {
        ReplaceDTO rp = this.rpService.getPost(replaceID);
        model.addAttribute("replace", rp);
        return "replace_detail";
    }

    //게시물을 작성할 때도 카테고리를 선택해서 게시물을 생성해야 한다. 
    //따라서 전체 카테고리 중에서 알맞는 카테고리를 선택할 수 있어야 한다. 즉, 아래와 같이 게시물 등록 폼에서 전체 카테고리를 조회한다.
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/create")
    public String Create(ReplaceForm Form, Model model) {
    	//model.addAttribute("categoryList", rpCategoryService.getList());
        return "replace_form";
    }

    //컨트롤러에 넘어온 카테고리 이름으로 카테고리 엔티티를 조회하고, 
    //조회한 카테고리 엔티티를 질문 엔티티에 넣어주고 저장하면 질문에 카테고리가 생기게 된다.
	/*
	 * @PreAuthorize("isAuthenticated()")
	 * 
	 * @PostMapping("/create") public String Create(Model model, @Valid ReplaceForm
	 * postForm, BindingResult bindingResult, Principal principal) { if
	 * (bindingResult.hasErrors()) { model.addAttribute("categoryList",
	 * rpCategoryService.getList()); return "replace_form"; } UserDTO User =
	 * this.userService.getUser(principal.getName()); ReplaceCategoryDTO category =
	 * this.rpCategoryService.getCategory(postForm.getReplaceCategoryID());
	 * this.rpService.create(postForm.getPostCategoryID(), postForm.getMbtiID(),
	 * postForm.getCityID(), postForm.getReplaceType(),
	 * postForm.getReplaceLocation(), postForm.getReplaceName(),
	 * postForm.getReplacePrice(), postForm.getReplaceContents(), postForm.getTel(),
	 * User,category); return "redirect:/replace/list"; }
	 */
    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<String> create(ReplaceDTO dto){
    	System.out.println(dto.toString());
    	for(MultipartFile file : dto.getFile()) {
    		System.out.println(file.getOriginalFilename());
    	}
    	return ResponseEntity.ok().body("Success message");
    }
    
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/modify/{id}")
    public String Modify(ReplaceForm postForm, @PathVariable("id") Integer adventureID, Principal principal) {
        ReplaceDTO rpDto = this.rpService.getPost(adventureID);
        if(!rpDto.getReplaceAdmin().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
        }
        postForm.setReplaceName(rpDto.getReplaceName());
        postForm.setReplaceContents(rpDto.getReplaceContents());
        return "replace_form";
    }
    
    //수정
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/modify/{id}")
    public String Modify(@Valid ReplaceForm postForm, BindingResult bindingResult, 
            Principal principal, @PathVariable("id") Integer replaceID) {
        if (bindingResult.hasErrors()) {
            return "replace_form";
        }
        ReplaceDTO rpDto = this.rpService.getPost(replaceID);
        if (!rpDto.getReplaceAdmin().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
        }
        this.rpService.modify(rpDto, postForm.getPostCategoryID(), postForm.getMbtiID(), postForm.getCityID(), postForm.getReplaceType(), postForm.getReplaceLocation(),
        		postForm.getReplaceName(), postForm.getReplacePrice(), postForm.getReplaceContents(), postForm.getTel());
        return String.format("redirect:/replace/detail/%s", replaceID);
    }
    
    //삭제
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/delete/{id}")
    public String Delete(Principal principal, @PathVariable("id") Integer replaceID) {
        ReplaceDTO rpDto = this.rpService.getPost(replaceID);
        if (!rpDto.getReplaceAdmin().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제권한이 없습니다.");
        }
        this.rpService.delete(rpDto);
        return "redirect:/";
    }
    
    //추천
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/suggestion/{id}")
    public String Suggestion(Principal principal, @PathVariable("id") Integer replaceID) {
        ReplaceDTO rpDto = this.rpService.getPost(replaceID);
        UserDTO user = this.userService.getUser(principal.getName());
        this.rpService.suggestion(rpDto, user);
        return String.format("redirect:/replace/detail/%s", replaceID);
    }	
	
}
