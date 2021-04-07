package com.slokashri.cicddemo.serviceimpl;

import com.slokashri.cicddemo.model.Member;
import com.slokashri.cicddemo.repo.MemberRepo;
import com.slokashri.cicddemo.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class MemberServiceImpl implements MemberService {
    @Autowired
    MemberRepo memberRepo;

    @Override
    public List<Member> listMembers() {
        return memberRepo.findAll();
    }
}
