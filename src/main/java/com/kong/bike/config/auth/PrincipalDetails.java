package com.kong.bike.config.auth;

// 시큐리티가 /login 주소요청이 오면 낚아채서 로그인을 진행시킨다.
// 로그인을 진행이 완료가 되면 시큐리티 session을 만들어준다. (Security ContextHolder)
// 오브젝트 타입 => Authentication 타입 객체
// Authentication 안에 User 정보가 있어야됨.
// User 오브젝트 타입 => UserDetails 타입 객체

// Security session => Authentication => UserDetails (PrincipalDetails)

import com.kong.bike.entity.MemberEntity;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;

@Getter
public class PrincipalDetails implements UserDetails{

    private MemberEntity memberEntity; // 콤포지션

    public PrincipalDetails(MemberEntity memberEntity){
        this.memberEntity = memberEntity;
    }

    // 해당 유저의 권한을 리턴하는 곳
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Collection<GrantedAuthority> collect = new ArrayList<>();
        collect.add(new GrantedAuthority() {
            @Override
            public String getAuthority() {
//                return "ROLE_"+joins.getRole();
                return memberEntity.getMemberRole();
            }
        });
        return collect;
    }

    // 패스워드를 리턴하는 곳
    @Override
    public String getPassword() {
        return memberEntity.getPassword();
    }

    @Override
    public String getUsername() {
        return memberEntity.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {

        return true;
    }

    // 너의 계정이 잠겼니
    @Override
    public boolean isAccountNonLocked() {

        return true;
    }

    // 로그인한지 1년이 지났니
    @Override
    public boolean isCredentialsNonExpired() {

        return true;
    }

    // 너의 계정이 활성화 되있니
    @Override
    public boolean isEnabled() {
        return true;

    }
}
