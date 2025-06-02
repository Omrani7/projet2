const axios = require('axios');

const API_BASE = 'http://localhost:8080/auth';

// Test users with varying compatibility levels for Faculté des sciences de Monastir
const testUsers = [
  // HIGH COMPATIBILITY (Same institute + same/similar field)
  {
    email: 'ahmed.ben.ali@fsm.rnu.tn',
    username: 'ahmed_fsm',
    password: 'Password123!',
    phoneNumber: '+21623456789',
    role: 'STUDENT',
    age: 22,
    profileDetails: {
      fullName: 'Ahmed Ben Ali',
      fieldOfStudy: 'Science informatique',
      institute: 'Faculté des sciences de Monastir',
      userType: 'STUDENT'
    }
  },
  {
    email: 'fatma.gharbi@fsm.rnu.tn',
    username: 'fatma_info',
    password: 'Password123!',
    phoneNumber: '+21624567890',
    role: 'STUDENT',
    age: 21,
    profileDetails: {
      fullName: 'Fatma Gharbi',
      fieldOfStudy: 'Informatique',
      institute: 'Faculté des sciences de Monastir',
      userType: 'STUDENT'
    }
  },
  {
    email: 'mohamed.sassi@fsm.rnu.tn',
    username: 'mohamed_cs',
    password: 'Password123!',
    phoneNumber: '+21625678901',
    role: 'STUDENT',
    age: 23,
    profileDetails: {
      fullName: 'Mohamed Sassi',
      fieldOfStudy: 'Génie informatique',
      institute: 'Faculté des sciences de Monastir',
      userType: 'STUDENT'
    }
  },
  // VERY GOOD COMPATIBILITY (Same institute + related fields)
  {
    email: 'sara.mahjoub@fsm.rnu.tn',
    username: 'sara_math',
    password: 'Password123!',
    phoneNumber: '+21628901234',
    role: 'STUDENT',
    age: 21,
    profileDetails: {
      fullName: 'Sara Mahjoub',
      fieldOfStudy: 'Mathématiques',
      institute: 'Faculté des sciences de Monastir',
      userType: 'STUDENT'
    }
  },
  {
    email: 'karim.bouaziz@fsm.rnu.tn',
    username: 'karim_physics',
    password: 'Password123!',
    phoneNumber: '+21629012345',
    role: 'STUDENT',
    age: 23,
    profileDetails: {
      fullName: 'Karim Bouaziz',
      fieldOfStudy: 'Physique',
      institute: 'Faculté des sciences de Monastir',
      userType: 'STUDENT'
    }
  },
  // MEDIUM COMPATIBILITY (Different institute + similar field)
  {
    email: 'rania.khemiri@enis.rnu.tn',
    username: 'rania_soft',
    password: 'Password123!',
    phoneNumber: '+21634567890',
    role: 'STUDENT',
    age: 22,
    profileDetails: {
      fullName: 'Rania Khemiri',
      fieldOfStudy: 'Génie logiciel',
      institute: 'École Nationale d\'Ingénieurs de Sfax',
      userType: 'STUDENT'
    }
  },
  {
    email: 'bilel.guesmi@esprit.tn',
    username: 'bilel_code',
    password: 'Password123!',
    phoneNumber: '+21635678901',
    role: 'STUDENT',
    age: 23,
    profileDetails: {
      fullName: 'Bilel Guesmi',
      fieldOfStudy: 'Informatique',
      institute: 'ESPRIT',
      userType: 'STUDENT'
    }
  },
  // LOWER COMPATIBILITY (Different institute + different field)
  {
    email: 'mariem.jlassi@fseg.rnu.tn',
    username: 'mariem_business',
    password: 'Password123!',
    phoneNumber: '+21637890123',
    role: 'STUDENT',
    age: 22,
    profileDetails: {
      fullName: 'Mariem Jlassi',
      fieldOfStudy: 'Business Administration',
      institute: 'Faculté des Sciences Économiques et de Gestion',
      userType: 'STUDENT'
    }
  }
];

async function createTestUsers() {
  console.log('🚀 Creating test users for roommate compatibility testing...\n');
  
  let successCount = 0;
  let errorCount = 0;
  
  for (const user of testUsers) {
    try {
      console.log(`Creating user: ${user.username} (${user.profileDetails.fullName})`);
      
      const response = await axios.post(`${API_BASE}/register`, user);
      
      if (response.status === 200 || response.status === 201) {
        console.log(`✅ Successfully created: ${user.username}`);
        successCount++;
      } else {
        console.log(`⚠️  Unexpected response for ${user.username}:`, response.status);
        errorCount++;
      }
    } catch (error) {
      console.log(`❌ Failed to create ${user.username}:`, error.response?.data?.message || error.message);
      errorCount++;
    }
    
    // Small delay to avoid overwhelming the server
    await new Promise(resolve => setTimeout(resolve, 500));
  }
  
  console.log('\n📊 Summary:');
  console.log(`✅ Successfully created: ${successCount} users`);
  console.log(`❌ Failed: ${errorCount} users`);
  console.log(`📝 Total attempted: ${testUsers.length} users`);
  
  if (successCount > 0) {
    console.log('\n🎯 Test the compatibility system by:');
    console.log('1. Login with your main account');
    console.log('2. Navigate to /roommates/compatible-students');
    console.log('3. See ML-powered recommendations based on academic profiles');
    console.log('\n🧠 Expected compatibility scores:');
    console.log('- Ahmed, Fatma, Mohamed: 90%+ (same institute + same field)');
    console.log('- Sara, Karim: 75%+ (same institute + related fields)');
    console.log('- Rania, Bilel: 50%+ (different institute + similar field)');
    console.log('- Mariem: 30%+ (different institute + different field)');
  }
}

// Run the script
createTestUsers().catch(console.error); 