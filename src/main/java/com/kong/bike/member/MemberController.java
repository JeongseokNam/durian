package com.kong.bike.member;

import com.kong.bike.DTO.BoardListDTO;
import com.kong.bike.DTO.PhoneListDTO;
import com.kong.bike.DTO.SearchDTO;
import com.kong.bike.board.service.BoardService;
import com.kong.bike.config.auth.PrincipalDetails;
import com.kong.bike.entity.*;
import com.kong.bike.file.service.FileService;
import com.kong.bike.like.service.LikeService;
import com.kong.bike.member.service.MemberService;
import com.kong.bike.notice.service.NoticeService;
import com.kong.bike.phone.service.PhoneService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Controller
public class MemberController {

    @Autowired
    private MemberService memberService;
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    private BoardService boardService;
    @Autowired
    private PhoneService phoneService;
    @Autowired
    private FileService fileService;
    @Autowired
    private NoticeService noticeService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private LikeService likeService;

    /*????????? ??????*/
    @GetMapping("/member/login") //????????????
    public String loginForm() {
        return "member/loginForm";
    }

    @PostMapping("/loginError") //????????? ??????
    public String loginFail(HttpServletRequest request, String username, String errorMsg) {
        return "member/loginForm";
    }

    @GetMapping("/member/logout") // ????????????
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) new SecurityContextLogoutHandler().logout(request, response, authentication);

        return "redirect:/member/login";
    }
    /*????????? ?????? END*/


    @GetMapping("/member/join")
    public String join() {
        return "member/joinMain";
    }
    @GetMapping("/member/joinNormal")
    public String normalJoinForm(){
        return "member/normalJoinForm";
    }
    @PostMapping("/member/joinNormal") // ????????????
    public String joinNormal(MemberEntity memberEntity) {
        memberEntity.setMemberRole("ROLE_USER");
        String rawPassword = memberEntity.getPassword();
        String encPassword = bCryptPasswordEncoder.encode(rawPassword);
        memberEntity.setPassword(encPassword);
        memberService.save(memberEntity);
        return "redirect:/member/joinSuccess";
    }

    @GetMapping("/member/pwdChange") // ????????? ???????????? ?????? ?????????
    public String pwdChangeForm(String phone,Model model){
        MemberEntity findMember = memberService.findByPhone(phone);
        model.addAttribute("memberInfo",findMember);
        return "member/pwdChange";
    }

    @ResponseBody
    @PostMapping("/member/idCheck") // ajax ????????? ????????????
    public String idCheck(String inputId) {
        /**
         * View ?????? ????????? ????????? ????????? ????????????
         * ???????????? : idCheck
         * ????????? : Jeongseok
         * ?????????  : 2022-06-09
         **/
        String flag = null;
        MemberEntity userInfo = memberService.getUserInfoByLoginId(inputId);
        if (userInfo != null) {
            flag = "true";
        }
        return flag;
    }

    @ResponseBody
    @PostMapping("/member/nickCheck") // ajax ????????? ????????????
    public String nickCheck(String inputNick) {
        String flag = null;
        MemberEntity userInfo = memberService.getUserInfoByNickName(inputNick);
        if (userInfo != null) {
            flag = "true";
        }
        return flag;
    }

    @GetMapping("/member/myPage") // ???????????????
    public String myPage(@AuthenticationPrincipal PrincipalDetails principalDetails, Model model) {
        MemberEntity memberEntity = principalDetails.getMemberEntity();

        // ------------????????? ????????? ??? & ??? ?????? ---------------------------
        List<BikeBoardEntity> boardList = boardService.getMyBoardList(memberEntity.getMemberId());
        List<BoardListDTO> totalList;
        totalList = boardList.stream().map(board -> modelMapper.map(board, BoardListDTO.class)).collect(Collectors.toList());
        for (int i = 0; i < totalList.size(); i++) { // ???????????? ??????
            FileEntity tempFile = fileService.findByRelatIdAndFileNumAndDelYnAndFileDiv(totalList.get(i).getBoardId(), 1 , "N","board");
            MemberEntity tempMember = memberService.findByMemberId(totalList.get(i).getMemberId());
            if (tempFile != null) {
                totalList.get(i).setFilePath(tempFile.getFilePath());
                totalList.get(i).setSaveFileNm(tempFile.getSaveFileNm());
            }
            totalList.get(i).setNickName(tempMember.getNickName());
        }
        model.addAttribute("totalList",totalList);
        model.addAttribute("memberInfo", memberEntity);

        /*????????? ????????? ?????????*/
        List<BoardListDTO> likeList = new ArrayList<>();
        List<LikeEntity> likeEntities = likeService.findByMemberEntity(memberEntity);
        for (int i = 0; i < likeEntities.size(); i++) {
            String relatDiv = likeEntities.get(i).getRelatDiv();
            if (relatDiv.equals("board")){
                BoardListDTO temp = new BoardListDTO();
                BikeBoardEntity bikeBoardEntity = boardService.getBoardInfo(likeEntities.get(i).getRelatId());
                BeanUtils.copyProperties(bikeBoardEntity.getMemberEntity(),temp);//????????????
                BeanUtils.copyProperties(bikeBoardEntity,temp);
                FileEntity tempFile = fileService.findByRelatIdAndFileNumAndDelYnAndFileDiv(likeEntities.get(i).getRelatId(), 1,"board" , "N");
                if (tempFile != null) {
                    temp.setFilePath(tempFile.getFilePath());
                    temp.setSaveFileNm(tempFile.getSaveFileNm());
                }
                likeList.add(temp);
            }
        }
        model.addAttribute("likeList",likeList);

        // ------------????????? ????????? ??? & ??? ?????? ---------------------------
        List<PhoneBoardEntity> phoneList = phoneService.getMyPhoneList(memberEntity.getMemberId());
        List<PhoneBoardEntity> PhoneTotalList;
        PhoneTotalList = phoneList.stream().map(phone -> modelMapper.map(phone, PhoneBoardEntity.class)).collect(Collectors.toList());
        for (int i = 0; i < PhoneTotalList.size(); i++) { // ???????????? ??????
            FileEntity tempFile = fileService.findByRelatIdAndFileNumAndDelYnAndFileDiv(PhoneTotalList.get(i).getPhoneId(), 1,"phone" , "N");
            if (tempFile != null) {
                totalList.get(i).setFilePath(tempFile.getFilePath());
                totalList.get(i).setSaveFileNm(tempFile.getSaveFileNm());
            }
        }
        model.addAttribute("PhoneTotalList",PhoneTotalList);
        model.addAttribute("memberInfo", memberEntity);

        /* ????????? ????????? ?????????*/
        List<PhoneBoardEntity> phoneLikeList = new ArrayList<>();
        for (int i = 0; i < likeEntities.size(); i++) {
            String relatDiv = likeEntities.get(i).getRelatDiv();
            if (relatDiv.equals("phone")){
                PhoneBoardEntity tempPhone = new PhoneBoardEntity();
                PhoneListDTO temp = new PhoneListDTO();
                BeanUtils.copyProperties(likeEntities.get(i).getMemberEntity(),tempPhone);//????????????
                BeanUtils.copyProperties(phoneService.getPhoneInfo(likeEntities.get(i).getRelatId()),tempPhone);
                FileEntity tempFile = fileService.findByRelatIdAndFileNumAndDelYnAndFileDiv(likeEntities.get(i).getRelatId(), 1,"phone" , "N");
                if (tempFile != null) {
                    temp.setFilePath(tempFile.getFilePath());
                    temp.setSaveFileNm(tempFile.getSaveFileNm());
                }
                phoneLikeList.add(tempPhone);
            }
        }
        model.addAttribute("phoneLikeList",phoneLikeList);
        return "member/myPage";
    }

    @GetMapping("/member/myPage/boardList") // ??????????????? -> ????????? ????????? ???????????????
    public String boardList(@AuthenticationPrincipal PrincipalDetails principalDetails, Model model
            , @PageableDefault(size = 3, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable) {
        MemberEntity memberEntity = principalDetails.getMemberEntity();
        Page<BikeBoardEntity> boardPageList =boardService.findByMyBoardList("N",memberEntity,pageable);
        List<BoardListDTO> boardList;
        boardList = boardPageList.stream().map(board -> modelMapper.map(board, BoardListDTO.class)).collect(Collectors.toList());
        for (int i = 0; i < boardList.size(); i++) { // ???????????? ??????
            MemberEntity tempMember = boardService.getBoardInfo(boardList.get(i).getBoardId()).getMemberEntity();
            FileEntity tempFile = fileService.findByRelatIdAndFileNumAndDelYnAndFileDiv(boardList.get(i).getBoardId(), 1, "N","board");
            if (tempFile != null) {
                boardList.get(i).setFilePath(tempFile.getFilePath());
                boardList.get(i).setSaveFileNm(tempFile.getSaveFileNm());
            }
            boardList.get(i).setNickName(tempMember.getNickName());
        }
        model.addAttribute("boardList",boardList);
        /*??????????????????*/
        model.addAttribute("boardPageList",boardPageList);
        model.addAttribute("previous", pageable.previousOrFirst().getPageNumber());
        model.addAttribute("next", pageable.next().getPageNumber());
        model.addAttribute("hasNext", boardPageList.hasNext());
        model.addAttribute("hasPrev", boardPageList.hasPrevious());
        return "member/boardList";
    }

    @GetMapping("/member/myPage/likeList") // ??????????????? -> ????????? ????????? ???????????????
    public String LikeList(@AuthenticationPrincipal PrincipalDetails principalDetails, Model model
            , @PageableDefault(size = 1, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable) {
        MemberEntity memberEntity = principalDetails.getMemberEntity();
        Page<BikeBoardEntity> boardLikeListPage = boardService.findByMyBoardList("N",memberEntity,pageable);
        List<BoardListDTO> likeList = new ArrayList<>();
        List<LikeEntity> likeEntities = likeService.findByMemberEntity(memberEntity);
        for (int i = 0; i < likeEntities.size(); i++) {
            String relatDiv = likeEntities.get(i).getRelatDiv();
            if (relatDiv.equals("board")) {
                BoardListDTO temp = new BoardListDTO();
                BikeBoardEntity bikeBoardEntity = boardService.getBoardInfo(likeEntities.get(i).getRelatId());
                BeanUtils.copyProperties(bikeBoardEntity.getMemberEntity(),temp);
                BeanUtils.copyProperties(bikeBoardEntity,temp);
                FileEntity tempFile = fileService.findByRelatIdAndFileNumAndDelYnAndFileDiv(likeEntities.get(i).getRelatId(), 1,"board" , "N");
                if (tempFile != null) {
                    temp.setFilePath(tempFile.getFilePath());
                    temp.setSaveFileNm(tempFile.getSaveFileNm());
                }

                likeList.add(temp);
            }
        }
        model.addAttribute("likeList",likeList);
        /*??????????????????*/
        model.addAttribute("boardLikeListPage",boardLikeListPage);
        model.addAttribute("previous", pageable.previousOrFirst().getPageNumber());
        model.addAttribute("next", pageable.next().getPageNumber());
        model.addAttribute("hasNext", boardLikeListPage.hasNext());
        model.addAttribute("hasPrev", boardLikeListPage.hasPrevious());
        return "member/LikeList";
    }

    @GetMapping("/member/myPage/phoneBoardList") // ??????????????? -> ????????? ????????? ???????????????
    public String phoneBoardList(@AuthenticationPrincipal PrincipalDetails principalDetails, Model model
            , @PageableDefault(size = 3, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable){

        MemberEntity memberEntity = principalDetails.getMemberEntity();
        Page<PhoneBoardEntity> phonePagingList;
        phonePagingList = phoneService.findMyPhoneBoardList("N",memberEntity,pageable);
        List<PhoneListDTO> phoneBoardList;
        phoneBoardList = phonePagingList.stream().map(phone -> modelMapper.map(phone, PhoneListDTO.class)).collect(Collectors.toList());
        for (int i = 0; i < phoneBoardList.size(); i++) { // ???????????? ??????
            MemberEntity tempMember = phoneService.getPhoneInfo(phoneBoardList.get(i).getPhoneId()).getMemberEntity();
            phoneBoardList.get(i).getMemberEntity().setNickName(tempMember.getNickName());
            FileEntity tempFile = fileService.findByRelatIdAndFileNumAndDelYnAndFileDiv(phoneBoardList.get(i).getPhoneId(), 1,"phone" , "N");
            if (tempFile != null) {
                phoneBoardList.get(i).setFilePath(tempFile.getFilePath());
                phoneBoardList.get(i).setSaveFileNm(tempFile.getSaveFileNm());
            }
        }
        model.addAttribute("phoneBoardList",phoneBoardList);
        /*??????????????????*/
        model.addAttribute("phonePagingList",phonePagingList);
        model.addAttribute("previous", pageable.previousOrFirst().getPageNumber());
        model.addAttribute("next", pageable.next().getPageNumber());
        model.addAttribute("hasNext", phonePagingList.hasNext());
        model.addAttribute("hasPrev", phonePagingList.hasPrevious());
        return "phone/phoneBoardList";
    }

    @GetMapping("/member/myPage/phoneLikeList") // ??????????????? -> ????????? ????????? ???????????????
    public String phoneLikeList(@AuthenticationPrincipal PrincipalDetails principalDetails, Model model
            , @PageableDefault(size = 1, sort = "createdDate", direction = Sort.Direction.DESC) Pageable pageable){
        MemberEntity memberEntity = principalDetails.getMemberEntity();
        Page<PhoneBoardEntity> phoneLikeListPage = phoneService.findMyPhoneBoardList("N", memberEntity, pageable);
        List<PhoneListDTO> likeList = new ArrayList<>();
        List<LikeEntity> likeEntities = likeService.findByMemberEntity(memberEntity);
        for (int i = 0; i < likeEntities.size(); i++) {
            String relatDiv = likeEntities.get(i).getRelatDiv();
            if (relatDiv.equals("phone")) {
                PhoneListDTO temp = new PhoneListDTO();
                PhoneBoardEntity phoneBoardEntity = phoneService.getPhoneInfo(likeEntities.get(i).getRelatId());
                BeanUtils.copyProperties(phoneBoardEntity.getMemberEntity(), temp);
                BeanUtils.copyProperties(phoneBoardEntity, temp);
                FileEntity tempFile = fileService.findByRelatIdAndFileNumAndDelYnAndFileDiv(likeEntities.get(i).getRelatId(), 1,"board" , "N");
                if (tempFile != null) {
                    temp.setFilePath(tempFile.getFilePath());
                    temp.setSaveFileNm(tempFile.getSaveFileNm());
                }
                likeList.add(temp);
            }
        }
        model.addAttribute("likeList",likeList);
        /*??????????????????*/
        model.addAttribute("phoneLikeListPage",phoneLikeListPage);
        model.addAttribute("previous", pageable.previousOrFirst().getPageNumber());
        model.addAttribute("next", pageable.next().getPageNumber());
        model.addAttribute("hasNext", phoneLikeListPage.hasNext());
        model.addAttribute("hasPrev", phoneLikeListPage.hasPrevious());
        return "phone/phoneLikeList";
    }


    @ResponseBody
    @PostMapping("/member/NickChange") // ??????????????? -> ??????????????? ????????????
    public void NickChange(@AuthenticationPrincipal PrincipalDetails principalDetails, String nickName){
        memberService.nickNameUpdate(principalDetails.getMemberEntity().getMemberId(), nickName);
        principalDetails.getMemberEntity().setNickName(nickName);
    }

    @ResponseBody
    @PostMapping("/member/emailChange") // ??????????????? -> ??????????????? ?????? ????????????
    public void emailChange(@AuthenticationPrincipal PrincipalDetails principalDetails, String inputEmail){
        memberService.emailUpdate(principalDetails.getMemberEntity().getMemberId(), inputEmail);
        principalDetails.getMemberEntity().setEmail(inputEmail);
    }

    @PostMapping("/member/PasswordChange") // ??????????????? -> ?????????????????? ?????? ????????????
    public String PwdChange(@AuthenticationPrincipal PrincipalDetails principalDetails, String password){
        MemberEntity memberEntity = memberService.passwordUpdate(principalDetails.getMemberEntity().getMemberId(), password);
        String rawPassword = memberEntity.getPassword();
        String encPassword = bCryptPasswordEncoder.encode(rawPassword);
        memberEntity.setPassword(encPassword);

        memberService.save(memberEntity);
        return "redirect:/member/myPage";
    }


    @ResponseBody
    @PostMapping("/member/pwdChange") // ??????????????? ???????????? ??????
    public String pwdChange(String inputPassword, Long memberId){
        MemberEntity tempMember = memberService.findByMemberId(memberId);
        String newPwd = bCryptPasswordEncoder.encode(inputPassword);
        tempMember.setPassword(newPwd);
        memberService.save(tempMember);
        return "true";
    }

    @GetMapping("/member/remove") // ???????????? ?????????
    public String removeForm(@AuthenticationPrincipal PrincipalDetails principalDetails, Model model) {
        MemberEntity memberEntity = principalDetails.getMemberEntity();
        model.addAttribute("memberEntity", memberEntity);
        return "member/remove";
    }

    @ResponseBody
    @PostMapping("/member/PwCheck") // ???????????? ??????
    public String PwCheck(@AuthenticationPrincipal PrincipalDetails principalDetails, String inputPw) {
        String memberPw = principalDetails.getMemberEntity().getPassword();
        boolean userPw = bCryptPasswordEncoder.matches(inputPw, memberPw);
        // System.out.println("inputPw:" + inputPw + "memberPw:" + memberPw + "userPw:" + userPw);
        if (userPw) {
            return "true";
        } else {
            return null;
        }
    }

    @PostMapping("/member/remove") // ???????????? ??????
    public String remove(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        memberService.updateDelMember(principalDetails.getMemberEntity().getMemberId());
        return "redirect:/member/logout";
    }

    @PostMapping("/member/changePhone") // ??????????????? ??????????????? ??????
    @ResponseBody
    public String changePhone(@AuthenticationPrincipal PrincipalDetails principalDetails, String phone, String name){
        MemberEntity tempMember = memberService.findByPhone(principalDetails.getMemberEntity().getPhone());
        if (tempMember.getRealName().equals(name)) {
            principalDetails.getMemberEntity().setPhone(phone);
            memberService.updatePhone(tempMember, phone);
            return "????????? ?????????????????????.";
        }else {
            return "????????? ???????????? ????????????.";
        }
    }

    @ResponseBody
    @PostMapping("/member/phoneChk")//?????? ????????? ????????? ????????????
    public String phoneChk(String phone){
        MemberEntity memberEntity = memberService.findByPhone(phone);
        if (memberEntity==null){
            return phone;
        }else {
            return null;
        }
    }
    @ResponseBody
    @PostMapping("/member/phoneChkForFindInfo")//?????? ????????? ????????? ????????????
    public String phoneChkForFindInfo(String phone){
        MemberEntity memberEntity = memberService.findByPhone(phone);
        if (memberEntity!=null){
            return phone;
        }else {
            return null;
        }
    }

    @GetMapping("/member/memberNotice") // ???????????? ?????????
    public String memberNotice(Model model
            , @PageableDefault(size = 3, sort = "createDate", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<NoticeEntity> noticePagingList = noticeService.findByDelYn(pageable);
        List<NoticeEntity> noticeList = noticePagingList.stream().map(notice -> modelMapper.map(notice, NoticeEntity.class)).collect(Collectors.toList());
        model.addAttribute("noticeList", noticeList);

        /*??????????????????*/
        model.addAttribute("noticePagingList",noticePagingList);
        model.addAttribute("previous", pageable.previousOrFirst().getPageNumber());
        model.addAttribute("next", pageable.next().getPageNumber());
        model.addAttribute("hasNext", noticePagingList.hasNext());
        model.addAttribute("hasPrev", noticePagingList.hasPrevious());
        return "member/memberNotice";
    }

    @GetMapping("/member/memberNoticeView") // ???????????? ???????????????
    public String memberNoticeView(Long noticeId, Model model) {
        NoticeEntity target = noticeService.findByNoticeId(noticeId);
        model.addAttribute("target", target);
        return "member/memberNoticeView";
    }

    @GetMapping("/member/joinSuccess")//???????????? ??????
    public String joinSuccess(){
        return "member/joinSuccess";
    }

}
