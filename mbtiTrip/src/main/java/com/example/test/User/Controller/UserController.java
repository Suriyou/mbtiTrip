package com.example.test.User.Controller;

import java.io.Console;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.catalina.security.SecurityConfig;
import org.hibernate.validator.internal.util.logging.Log;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import com.example.test.GCSService.GCSService;
import com.example.test.User.DAO.UserDAO;
import com.example.test.User.DAO.UserHistoryDAO;
import com.example.test.User.DTO.QnADTO;
import com.example.test.User.DTO.UserDTO;
import com.example.test.User.DTO.User_Role;
import com.example.test.User.Service.CustomLoginService;
import com.example.test.User.Service.QnAService;
import com.example.test.User.Service.UXService;
import com.example.test.User.Service.UserHistoryService;
import com.example.test.User.Service.UserHistoryService;
import com.example.test.User.Service.UserService;
import com.example.test.User.Service.UserServiceImpl;
import com.example.test.item.DAO.ItemDAO;
import com.example.test.item.DTO.ItemDTO;

import groovy.transform.ToString;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Controller
public class UserController {

	/**
	 * @author Shin Sung Jin 
	 * User와 관련된 전반적인 기능을 담당하는 Controller입니다.  
	 * */
	
	@Autowired
	private UserService userService; 
	
	@Autowired
	private QnAService qnaService;
	
	@Autowired
	private CustomLoginService loginservice;
	
	@Autowired
	private UserHistoryService userHistoryService;
	
	@Autowired
	private UserDAO userDAO;
	
	@Autowired
	private UXService uxService;

	private BCryptPasswordEncoder bcrypasswordEncoder = new BCryptPasswordEncoder(); 
	
//	@RequestMapping(value="/access_denied_page", method=RequestMethod.GET)
//	public String DeniedPage  {       
//		return "access_denied_page";  
//	}
	
	/*추후 진행*/
//	@RequestMapping("globalecptionTest") 
//	public void globalExceptionTest() {
//		new ErrorRespone(ErrorCode.updateFailException);
//	}
	
	
	/*Guest Main Page*/
	@RequestMapping(value ="/" , method = RequestMethod.GET)
	public ModelAndView main(ModelAndView mv) {
		
		/** @autor 신성진
		 * guest메인에 들어온 사용자가 권한을 가지고 있거나, RememberMe 기능을 이용하거나
		 * 이전에 이용하던 세션 정보가 남아있는 경우 
		 * 각 권한에 맞는 main으로 redirect시킵니다. 
		 */
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if(auth!= null && auth.isAuthenticated()) {
			log.info("RememberMe User =>{}", auth);
			for(GrantedAuthority au : auth.getAuthorities()) {
				switch(au.getAuthority()) {
				case "ROLE_USER" :  mv.setViewName("redirect:/user/main"); break;
				case "ROLE_BIS" :  mv.setViewName("redirect:/bis/main"); break;
				case "ROLE_ANONYMOUS": 	mv.setViewName("main"); break;
				}
			}
		}	
		return mv;
	}
	
	
	/** @autor 신성진
	 *  일반 사용자 Main 페이지입니다. 다양한 UX 관련한 기능을 지원합니다. 
	 */
	@PreAuthorize("isAuthenticated() and  hasRole('ROLE_USER')")
	@RequestMapping(value = "/user/main", method = RequestMethod.GET)
	public ModelAndView main(Principal principal,
							Authentication auth,
							ModelAndView mav) {
		
		//UID를 이용하여, user의 전체 정보 조회
		Integer UID = userService.princeUID(principal);
		Map<String, Object> user = userService.getInfo(UID);
		
		//UX 기능을 위한 userName 선언
		String userName = userDAO.getUserNameByuserID(principal.getName());
		
		//UserUXS => 사용자의 활동 기록을 탐색하고, 그에 맞는 여행 루틴 추천 
		List<HashMap<String, Object>> UserUXsBefore = userHistoryService.uxRutin(userName);
		List<HashMap<String, Object>> UserUXs = uxService.insertUrls(UserUXsBefore);
		
		//사용자 정보가 충분하지 않을때, 정보를 제공하는 것보단, 조금 더 활동을 쌓길 권장합니다. 
		if(UserUXs.get(0).isEmpty() || UserUXs.size() <3) {
			log.info("사용자 정보가 충분하지 않다");
			mav.addObject("UxMessage", "사용자 정보가 충분하지 않습니다.");
		}
		
		//UserUXS => 사용자의 활동 기록을 탐색하고, 그에 맞는 숙소 정보 추천 
		List<HashMap<String, Object>> userUxReBefore = userHistoryService.uxReplace(userName);
		List<HashMap<String, Object>> userUxRe = uxService.insertUrls(userUxReBefore);
		if(userUxRe.get(0).isEmpty() || UserUXs.size() <3) {
			
			mav.addObject("UxReMessage", "사용자 정보가 충분하지 않습니다.");
		}
		
		//UserUXS => 사용자의 활동 기록을 탐색하고, 그에 맞는 여행지 정보 추천 
		List<HashMap<String, Object>> userUxADBefore = userHistoryService.uxAdventure(userName);
		List<HashMap<String, Object>> userUxAD = uxService.insertUrls(userUxADBefore);
		if(userUxAD.get(0).isEmpty() || UserUXs.size() <3) {
		
			mav.addObject("UxAdMessage", "사용자 정보가 충분하지 않습니다.");
		}

		
		//그동안 사용자가 본 게시글, 숙소, 어드벤처 정보를 다 담고 있음 
		List<List<?>> userViewInfo =userHistoryService.userViewInfo(principal);
		log.info("userviewINfo 조회 끝 {}", userViewInfo);
		if(userViewInfo.get(0).isEmpty() || userViewInfo.get(1).isEmpty() || userViewInfo.get(2).isEmpty()) {
			log.info("사용자가 조회한 정보 없음");
			mav.addObject("viewErrorMessage", "사용자가 조회한 정보 없음");
		}
		else {
			mav.addObject("userViewInfo",userViewInfo);
		}
		
		log.info("userUxAD 조회 결과  {}", userUxAD);
		mav.addObject("UserUXs",UserUXs);
		mav.addObject("userUxRe",userUxRe);
		mav.addObject("userUxadv",userUxAD);
		mav.addObject("user", user);
		mav.setViewName("User_Main");
		return mav;
	}
	
	
//	/*UX 관련 테스트 진행 완료, TH 문법 적용 완료 */
//	@RequestMapping("usertest")
//	public ModelAndView main(Principal principar, ModelAndView mav) {
//		String userName = principar.getName();
//		
//		List<HashMap<String, Object>> userUX = userHistoryService.uxRutin(userName);
//		for(HashMap<String, Object> user : userUX) {
//			log.info(user);
//		}
//		
//		List<HashMap<String, Object>> userUXreplace = userHistoryService.uxReplace(userName);
//		for(HashMap<String, Object> userre : userUXreplace) {
//			log.info(" ");
//			log.info("message userRE ====>{}",userre);
//		}
//			
//		List<HashMap<String, Object>> userPlace = userHistoryService.uxAdventure(userName);
//		for(HashMap<String, Object> userad : userPlace) {
//			log.info(" ");
//			log.info("message userRE ====>{}",userad);
//		}
//		mav.addObject("UserUXs", userUX);
//		mav.addObject("userUxRe", userUXreplace);
//		mav.addObject("userUxadv", userPlace);
//		mav.setViewName("MainTest");
//		return mav;
//	}
	

	/** 회원가입시, 일반사용자인지, 비즈니스 사용자인지 선택이 가능합니다. 
	 * */
	@RequestMapping(value = "/signup", method=RequestMethod.GET)
	public String signUpSelect() {
		return "sign_up_select";
	}
	
	/** 일반 사용자 회원가입 페이지입니다. 
	 * */
	@RequestMapping(value = "/user/signup", method = RequestMethod.GET)
	public ModelAndView signUpUser(HttpServletRequest request) {
	    ModelAndView mav = new ModelAndView();
	    String currentUrl = request.getRequestURI().toString();
	    mav.addObject("currentUrl", currentUrl);
	    mav.setViewName("sign_up");
	    return mav;
	}
	
	@RequestMapping(value = "/user/signup", method=RequestMethod.POST)
	public ModelAndView singupUser(@RequestBody UserDTO userdto , ModelAndView mav) {		
		//ModelAndView mav = new ModelAndView();     // 아직 비번 암 복호화 안됌 ㅋㅌ
		log.info("signup user,Post도착 ");
		log.info("UserDetail :  {}", userdto);
		userdto.setUserrole(User_Role.user.getValue());   
		int result = userService.createUser(userdto);
		boolean chk = false;
		if(result == 1) {
			chk = true;
			mav.addObject(chk);
			mav.setViewName("redirect:/login_A");
		} 
		mav.addObject(chk);
		return mav;
	}  

	/*security에 사용될 기본 로그인 페이지 경로입니다. */
	@RequestMapping(value = "/login_A", method=RequestMethod.GET)
	public ModelAndView login() {
		ModelAndView mav = new ModelAndView();
		mav.setViewName("login_form");
		return mav;
	}
	
	/*User login proccess 입니다. */
	@RequestMapping(value = "user/login/success")
	public ModelAndView UserSuccess(ModelAndView mav, 
								Principal princ) {
		Integer userUID = userService.princeUID(princ);
		Map<String, Object> user = userService.getInfo(userUID);
		log.info("UserLoginSuccess = UserINFo= {}", user);
		mav.addObject("user", user);
		mav.setViewName("redirect:/user/main");
		return mav;
	}

	/*userMain에서 제공하는 지역 기반 Item 검색 기능입니다.*/
	@RequestMapping(value = "/searchLocation", method=RequestMethod.POST, produces = "text/plain;charset=UTF-8")
	public ModelAndView searchLocation(@RequestParam("location") String location, ModelAndView mav) {
		List<ItemDTO> result = userService.serchLocation(location);
		mav.addObject("list",result);
		mav.setViewName("itemList");
		return mav;
	}
	

	
//	@RequestMapping(value="/user/{UID}", method = RequestMethod.GET)
//	public ModelAndView mainUser(@PathVariable Integer UID , UserDTO userDTO, ModelAndView mav) {
//		// userMBTI에 따른 replace, adventure 정보 
//		// 인기 POSt 정보, 
//		// 
//		mav.addObject("user", user);
//		
//		return mav;
//	}
	
		
//	@RequestMapping(value = "/user/mypage/{UID}", method = RequestMethod.GET)
//	public ModelAndView mypageUser(@PathVariable("UID") Integer UID, UserDTO userdto, ModelAndView mav){
//	
//		Map<String, Object> map = userService.getInfo(UID);
//		System.out.println("mypageLoad="+map.toString());
//		mav.addObject("map", map);
//		mav.setViewName("mypage");
//		return mav;
//	}
	
	/*UserMypage입니다. */
	@PreAuthorize("isAuthenticated() and  hasRole('ROLE_USER')")
	@RequestMapping(value = "/user/mypage", method = RequestMethod.GET)
	public ModelAndView mypageUser(Principal principal, UserDTO userdto, ModelAndView mav){
		// 사용자 정보 조회
		Integer UID = userService.princeUID(principal);
		Map<String, Object> user = userService.getInfo(UID);
		
		//사용자가 작성한 Post 정보 조회
		List<HashMap<String, Object>> userPost =userHistoryService.selectUserPost(principal);
		log.info("userPost ===>{}", userPost);
		
		//사용자가 작성한 QnA 정보 조회
		List<HashMap<String, Object>> userQnA = userHistoryService.selectUserQnA(principal);
		log.info("userQnA ===>{}", userQnA);
		mav.addObject("user", user);
		mav.addObject("userPosts", userPost);
		mav.addObject("userQnA", userQnA);
		mav.setViewName("mypage");
		return mav;
	}

	
	//사용자가 자신의 정보를 수정하기 전 password 검증을 받게 합니다.
	@PreAuthorize("isAuthenticated() and  hasRole('ROLE_USER')")
	@RequestMapping(value = "/user/mypage/update", method = RequestMethod.GET)
	public ModelAndView update_ck(Principal principal, ModelAndView mav){
		log.info("cheak");
		mav.addObject("userName", principal.getName());
		mav.setViewName("user_update_ck");
		return mav;
	}
	
	@PreAuthorize("isAuthenticated() and  hasRole('ROLE_USER')")
	@RequestMapping(value = "/user/mypage/update", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<String> update_ck(@RequestBody String password,    
								Principal principal, ModelAndView mav) throws Exception{
		log.info("message ={}", principal.getName());
		boolean passwordCheck = userService.passwordCK(principal, password);
		if(passwordCheck) {
			log.info("message 인증성공");
			mav.addObject("userName", principal.getName());
			mav.setViewName("redirect:/user/mypage/update/ck");	
		}
		else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("비밀번호가 일치하지 않습니다.");
			//throw new Exception("비밀번호가 일치 하지 않습니다.");
		}
		return ResponseEntity.ok("비밀번호가 일치합니다.");
	} 
	

	// password 검증이 된 사용자가 자신의 정보를 수정할 수 있습니다. 
	@PreAuthorize("isAuthenticated() and  hasRole('ROLE_USER')")
	@RequestMapping(value = "/user/mypage/update/ck", method = RequestMethod.GET)
	public ModelAndView update(Principal principal, UserDTO userdto, ModelAndView mav){
		Integer UID = userService.princeUID(principal);		
		Map<String, Object> map = userService.getInfo(UID);
		mav.addObject("map", map);
		mav.setViewName("user_update");
		return mav;
	}
	
	
	@PreAuthorize("isAuthenticated() and  hasRole('ROLE_USER')")
	@RequestMapping(value = "/user/mypage/update/ck", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<String> update(UserDTO userdto,
								Principal principal, ModelAndView mav) {
		log.info("message POST ONE ={}", userdto.toString());
		try {
			int result= userService.userUpdate(userdto, principal);
			System.out.println("result : "+result);
			if(result != 0) {
				/*
				 * Map<String, Object> user = userService.getInfo(userdto.getUID());
				 * mav.addObject(user); mav.addObject("message", "회원정보가 수정 되었습니다");
				 * mav.setViewName("redirect:/user/mypage");
				 */
				return ResponseEntity.ok("회원정보가 수정 되었습니다");
			}
			else {
				log.info("post, else");
				mav.addObject(userdto);
				throw new Exception("정보가 정상적으로 수정되지 않았습니다");	
			}
		}

		catch (Exception e) {
			mav.addObject("message", e.getClass());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("회원정보를 다시 확인해주세요");
		}
		
	}
	
	
	/**
	 * 로그인 테스트를 위한 url입니다. 추후 기능 고도화시 필요할 것 같아 남겨둡니다.
	 * */
	@PreAuthorize("isAuthenticated()")
	@RequestMapping("/check-login")
    public ResponseEntity<UserDTO> loginCheck(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }else {
        	UserDTO userDTO = userService.getUser(principal.getName());
        	// 로그인된 사용자에 대한 처리 추가
            return ResponseEntity.ok(userDTO); // 로그인된 사용자의 경우 OK 상태 반환
        }
        
    }
	@GetMapping("/howUse")
	public String howUse() {
		return "howToUse";
	}
	
	
}











/*시큐리티 도입으로 인해 폐기 */
//@RequestMapping(value = "/login_A", method=RequestMethod.POST)
//public String login(@ModelAttribute UserDTO userdto, Model model) {
//	System.out.print(userdto.toString());
//	 Map<String, Object> user = userService.login(userdto);
//	 System.out.print("user정보 저장 map = "+user.toString());
//	 model.addAttribute("user", user);
//	 try {
//		 if(user.get("UID")  != null) { 
//			 
//			 model.addAttribute(user);
//			 System.out.println(User_Role.user.toString());
//			 if(user.get("userrole").equals(User_Role.user.getValue())) {
//				 System.out.println("유저 로그인 정보 조회 성공");
//				
//				 return String.format("redirect:user/main/%s", user.get("UID"));
////				 return String.format("redirect:/main/%s/%s", user.get("userrole") ,user.get("UID"));
////				 return String.format("redirect:/user/%s", user.get("UID"));
//			 	}
//			 else if(user.get("userrole").equals(User_Role.bis.getValue())) {
//				 return String.format("redirect:bis/main/%s", user.get("UID"));
////				 return String.format("redirect:/bis/%s", user.get("UID"));
//			 	}	 
//			 else if(user.get("userrole").equals(User_Role.admin.getValue())) {
//				 return "redirect:/main";
////				 return String.format("redirect:/admin/%s", user.get("UID"));
//			 }
//		   }
//		 else {
//			 model.addAttribute("message", "사용자 정보를 찾을 수 없습니다.");
//			 return "redirect:/login_A";
//		}
//	} catch (Exception e) {
//		model.addAttribute("message", e);
//		e.printStackTrace();
//
//		return "redirect:/login_A";
//	}
//	return "redirect:/login_A";
//}
