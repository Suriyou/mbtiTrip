package com.example.test.User.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.example.test.User.DTO.UserDTO;
import com.example.test.User.DTO.User_Role;
import com.example.test.User.Service.UserCartService;
import com.example.test.User.Service.UserHistoryService;
import com.example.test.User.Service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;


@Controller
@RequestMapping("/bis")
@Log4j2
public class BisController {
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private UserCartService userCartService;
	
	@Autowired
	private UserHistoryService userHistoryservice;
	
//	@RequestMapping(value="/{UID}", method = RequestMethod.GET)
//	public ModelAndView mainUser(@PathVariable Integer UID , UserDTO userDTO, ModelAndView mav) {
//		// 사용자가 등록해둔 replace, adventure 정보
		// replace를 조회한 사람, 장바구니에 넣어둔 사람, 이벤트 정보, 누적 인기 순위? 
//		// 
//		mav.addObject("user", user);s
//		
//		return mav;
//		@RequestMapping(value = "/signup/bis", method=RequestMethod.GET)
	
	/*Bis 회원가입*/
	@RequestMapping(value = "/signup", method = RequestMethod.GET)
	public ModelAndView signUpBis(HttpServletRequest request) {
		ModelAndView mav = new ModelAndView();
		String currentUrl = request.getRequestURI().toString();
		mav.addObject("currentUrl", currentUrl);
		mav.setViewName("sign_up");
		return mav;
	}
	
	@RequestMapping(value = "/signup", method=RequestMethod.POST)
	@ResponseBody
	public boolean singupUserBis(@RequestBody UserDTO userdto
			 					) {
		log.info("Bunm ===>{}", userdto.getBisNum());
		//ModelAndView mav = new ModelAndView();    
		log.info("BisDTO get  ==>{}", userdto);
		userdto.setUserrole(User_Role.bis.getValue());
		log.info("userDTO BIS  SignUP ===>", userdto);
		int result = userService.createBis(userdto);
		boolean chk = false;
		if(result == 1) {
			chk = true;
			//mav.addObject(result);
		} 
		return chk;
	}
	
//	@RequestMapping(value = "/login/success")
//	public ModelAndView UserSuccess(ModelAndView mav, 
//								Principal princ) {
//		String userName = princ.getName();
//		Integer userUID = userService.findByUID(userName);
//		Map<String, Object> user = userService.getInfo(userUID);
//		log.info("UserLoginSuccess = UserINFo= {}", user);
//		mav.addObject("user", user);
//		mav.setViewName("Bis_main");
//		return mav;
//	}
	
	/*bis 로그인 성공 프로세스*/
	@RequestMapping(value = "/login/success")
	public String UserSuccess() {
		return "redirect:/bis/main";
	}
	
	
	@PreAuthorize("hasRole('ROLE_BIS') or hasRole('ROLE_ADMIN')")
	@RequestMapping(value = "/main", method = RequestMethod.GET)
	public ModelAndView main(Principal principal,
							ModelAndView mav) {
		log.info("main 접속 중 ");
		/*bis 정보 찾기*/
		Integer UID = userService.findByUID(principal);
		Map<String, Object> bis = userService.getInfo(UID);
		log.info("userMain info = {} ", bis);
		
		/*bis User가 등록한 Item 과 , 조회수 찾기*/
		List<HashMap<String, Object>> userItems = userService.getMyItem(principal);
		List<HashMap<String, Object>> viewRating = userHistoryservice.viewRating(principal);
		
		/*bis Item에 조회수 삽입*/
		userService.bisListput(userItems, viewRating);
		
		mav.addObject("user", bis);
		mav.addObject("userItems", userItems);
		mav.setViewName("Bis_main");
		return mav;
	}
	
	
	@PreAuthorize("hasRole('ROLE_BIS') or hasRole('ROLE_ADMIN')")
	@RequestMapping(value = "/mypage", method = RequestMethod.GET)
	public ModelAndView mypageBis(Principal principal, UserDTO userdto, ModelAndView mav){
		/*bis 정보 조회*/
		Integer userUID = userService.findByUID(principal);
		Map<String, Object> user = userService.getInfo(userUID);
		List<HashMap<String, Object>> userItems = userService.getMyItem(principal);
		List<HashMap<String, Object>> viewRating = userHistoryservice.viewRating(principal);
		userService.bisListput(userItems, viewRating);
		
		/*User 예약 현황 불러오기 */
		List<HashMap<String, Object>> userReservation = 
				userCartService.reservationInfo(principal);
		log.info("BisController UserReservation INfo => {}", userReservation);
		mav.addObject("user", user);
		mav.addObject("userItems", userItems);
		mav.addObject("userReservation",userReservation);
		mav.setViewName("Bis_MyPageTest");
		return mav;
	}
	
	
//	@RequestMapping(value = "/mypage/update/", method = RequestMethod.GET)
//	public ModelAndView update(Principal principal, UserDTO userdto, ModelAndView mav){
////		String Uid = userdto.getUID();
////		mav.addObject(userdto);
//// 0321 최우진 유저데이터가 들어가야 될거같아서 주석하고 밑으로 바꿔봄
//		Integer UID = userService.princeUID(principal);		
//		Map<String, Object> map = userService.getInfo(UID);
//		mav.addObject("map", map);
//		mav.setViewName("user_update");
//		return mav;
//	}
	
	
	@PreAuthorize("isAuthenticated()")
	@RequestMapping(value = "/mypage/update", method = RequestMethod.GET)
	public ModelAndView update_ck(Principal principal, ModelAndView mav){
		mav.addObject("userName", principal.getName());
		mav.setViewName("user_update_ck");
		return mav;
	}
	
	@RequestMapping(value = "/mypage/update", method = RequestMethod.POST) 
	public ModelAndView update_ck(@RequestParam("password") String password,    
								Principal principal, ModelAndView mav) throws Exception{
		log.info("message ={}", principal.getName());
		
		boolean passwordCheck = userService.passwordCK(principal, password);
		if(passwordCheck) {
			log.info("message 인증성공");
			mav.addObject("userName", principal.getName());
			mav.setViewName("redirect:/user/mypage/update/ck");	
		}
		else {
			throw new Exception("비밀번호가 일치 하지 않습니다.");
		}
		return mav;
	} 
	
	@PreAuthorize("isAuthenticated()")
	@RequestMapping(value = "/mypage/update/ck", method = RequestMethod.GET)
	public ModelAndView Bisupdate(Principal principal, UserDTO userdto, ModelAndView mav){
		Integer UID = userService.princeUID(principal);		
		Map<String, Object> map = userService.getInfo(UID);
		List<HashMap<String, Object>> myItem = userService.getMyItem(principal);
		mav.addObject("map", map);
		mav.addObject("myItem", myItem);
		mav.setViewName("user_update");
		return mav;
	}
	
	
	
	@RequestMapping(value = "/mypage/update/ck", method = RequestMethod.POST)
	public ModelAndView Bisupdate(@ModelAttribute UserDTO userdto, ModelAndView mav) {
		try {
			int result= userService.BisUpdate(userdto);
			if(result == 1) {
				mav.addObject(userdto);
				mav.addObject("message", "회원정보가 수정 되었습니다");
				mav.setViewName(String.format("redirect:bis/mypage/%s", userdto.getUID()));
			}
			else {
				throw new Exception();
			}
		} catch (Exception e) {
			mav.addObject("message", e.getClass());
			mav.setViewName("mypage");
		}
		return mav;
	}
	
	
	
}
